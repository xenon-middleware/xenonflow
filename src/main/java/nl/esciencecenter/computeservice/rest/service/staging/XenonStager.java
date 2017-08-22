package nl.esciencecenter.computeservice.rest.service.staging;

import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.model.JobState;
import nl.esciencecenter.computeservice.rest.service.JobService;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.CopyMode;
import nl.esciencecenter.xenon.filesystems.CopyStatus;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class XenonStager {
	private FileSystem localFileSystem;
	private FileSystem remoteFileSystem;
	private JobService jobService;
	private JobRepository repository;

	public XenonStager(JobService jobService, JobRepository repository, FileSystem localFileSystem, FileSystem remoteFileSystem) {
		this.localFileSystem = localFileSystem;
		this.remoteFileSystem = remoteFileSystem;
		this.jobService = jobService;
		this.repository = repository;
	}

	/**
	 * Stages everything defined in the StagingManifest
	 * 
	 * - FileStagingObject are used to stage files from the sourceFileSystem to
	 * the targetFileSystem - StringToFileStagingObject are used to stage a
	 * literal string to the targetFileSystem
	 * 
	 * @param manifest
	 * @param sourceFileSystem
	 * @param targetFileSystem
	 * @throws Exception 
	 */
	public void doStaging(StagingManifest manifest, FileSystem sourceFileSystem, FileSystem targetFileSystem)
			throws Exception {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + manifest.getJobId());

		// Make sure the target directory exists
		Path targetDirectory = targetFileSystem.getWorkingDirectory().resolve(manifest.getTargetDirectory());
		if (!targetFileSystem.exists(targetDirectory)) {
			jobLogger.info("Creating directory: " + targetDirectory);
			targetFileSystem.createDirectories(targetDirectory);
		}

		// Copy all the files there
		for (StagingObject stageObject : manifest) {
			if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				Path targetPath = targetDirectory.resolve(object.getTargetPath());

				jobLogger.info("Copying from " + sourcePath + " to " + targetPath);
				String copyId = sourceFileSystem.copy(sourcePath, targetFileSystem, targetPath, CopyMode.REPLACE,
						false);
				waitForCopy(copyId, sourceFileSystem, manifest);
			} else if (stageObject instanceof StringToFileStagingObject) {
				StringToFileStagingObject object = (StringToFileStagingObject) stageObject;
				Path targetPath = targetDirectory.resolve(object.getTargetPath());
				jobLogger.info("Writing string to: " + targetPath);
				PrintWriter out = new PrintWriter(targetFileSystem.writeToFile(targetPath));
				String contents = object.getSourceString();
				jobLogger.debug("Input contents: " + contents);
				out.write(contents);
				out.close();
			} else if (stageObject instanceof DirectoryStagingObject) {
				DirectoryStagingObject object = (DirectoryStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				Path targetPath = targetDirectory.resolve(object.getTargetPath());

				jobLogger.info("Copying from " + sourcePath + " to " + targetPath);
				String copyId = sourceFileSystem.copy(sourcePath, targetFileSystem, targetPath, CopyMode.REPLACE,
						true);
				waitForCopy(copyId, sourceFileSystem, manifest);
			}
		}
	}

	private void waitForCopy(String copyId, FileSystem sourceFileSystem, StagingManifest manifest) throws Exception {
		CopyStatus s = sourceFileSystem.waitUntilDone(copyId, 1000);
		Job job = repository.findOne(manifest.getJobId());
		while (!job.getInternalState().isCancellationActive() && !s.isDone()) {
			job = repository.findOne(manifest.getJobId());
			s = sourceFileSystem.waitUntilDone(copyId, 1000);
		}

		if (job.getInternalState().isCancellationActive()) {
			jobService.setJobState(job.getId(), job.getInternalState(), JobState.CANCELLED);
			return;
		}

		if (s.hasException()) {
			throw s.getException();
		}
	}

	/**
	 * Do the staging from local to remote
	 * 
	 * @param manifest
	 * @return 
	 * @throws Exception 
	 */
	public void stageIn(StagingManifest manifest) throws Exception {
		doStaging(manifest, localFileSystem, remoteFileSystem);
	}

	/**
	 * Do the staging from remote to local and update the job.
	 * 
	 * FileStagingObject is used to stage files from remote to local -
	 * FileToStringStagingObject is used to read a remote file into a string in
	 * the job output - FileToMapStagingObject is used to read a remote file
	 * into a json object in the job output
	 * 
	 * @param manifest
	 * @throws Exception 
	 */
	public void stageOut(StagingManifest manifest) throws Exception {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + manifest.getJobId());

		try {
			doStaging(manifest, remoteFileSystem, localFileSystem);
		} catch (XenonException e) {
			jobService.setErrorAndState(manifest.getJobId(), e, JobState.STAGING_OUT, JobState.PERMANENT_FAILURE);
			return;
		}

		for (StagingObject stageObject : manifest) {
			String outputTarget = null;
			Object outputObject = null;
			if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				UriComponentsBuilder b = UriComponentsBuilder.fromUriString(localFileSystem.getLocation().toString());

				b.pathSegment(manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
				outputTarget = object.getTargetPath().getFileNameAsString();
				outputObject = b.build().toString();
			} else if (stageObject instanceof DirectoryStagingObject) {
				DirectoryStagingObject object = (DirectoryStagingObject) stageObject;
				UriComponentsBuilder b = UriComponentsBuilder.fromUriString(localFileSystem.getLocation().toString());

				b.pathSegment(manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
				outputTarget = object.getTargetPath().getFileNameAsString();
				outputObject = b.build().toString();
			} else if (stageObject instanceof FileToStringStagingObject) {
				FileToStringStagingObject object = (FileToStringStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				String contents = IOUtils.toString(remoteFileSystem.readFromFile(sourcePath), "UTF-8");
				jobLogger.debug("Read contents from " + sourcePath + ": " + contents);
				outputTarget = object.getTargetString();
				outputObject = contents;
			} else if (stageObject instanceof FileToMapStagingObject) {
				FileToMapStagingObject object = (FileToMapStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				String contents = IOUtils.toString(remoteFileSystem.readFromFile(sourcePath), "UTF-8");
				jobLogger.debug("Read contents from " + sourcePath + ": " + contents);

				if (contents.length() > 0) {
					ObjectMapper mapper = new ObjectMapper(new JsonFactory());
					TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
					};
					HashMap<String, Object> value = mapper.readValue(new StringReader(contents), typeRef);

					outputTarget = object.getTargetString();
					outputObject = value;
				}
			}
			jobService.setOutput(manifest.getJobId(), outputTarget, outputObject);
		}
	}
}

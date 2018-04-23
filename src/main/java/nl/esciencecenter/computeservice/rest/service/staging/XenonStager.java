package nl.esciencecenter.computeservice.rest.service.staging;

import java.io.IOException;
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
import nl.esciencecenter.computeservice.rest.model.StatePreconditionException;
import nl.esciencecenter.computeservice.rest.model.WorkflowBinding;
import nl.esciencecenter.computeservice.rest.service.JobService;
import nl.esciencecenter.computeservice.rest.service.XenonService;
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
	private XenonService service;

	public XenonStager(JobService jobService, JobRepository repository, FileSystem localFileSystem, FileSystem remoteFileSystem, XenonService service) {
		this.localFileSystem = localFileSystem;
		this.remoteFileSystem = remoteFileSystem;
		this.jobService = jobService;
		this.repository = repository;
		this.service = service;
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
	 * @throws XenonException 
	 * @throws StatePreconditionException 
	 */
	public void doStaging(StagingManifest manifest, FileSystem sourceFileSystem, FileSystem targetFileSystem) throws XenonException, StatePreconditionException {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + manifest.getJobId());

		// Make sure the target directory exists
		Path targetDirectory = targetFileSystem.getWorkingDirectory().resolve(manifest.getTargetDirectory()).toAbsolutePath();
		if (!targetFileSystem.exists(targetDirectory)) {
			jobLogger.info("Creating directory: " + targetDirectory);
			targetFileSystem.createDirectories(targetDirectory);
		}
		
		Path sourceDirectory = sourceFileSystem.getWorkingDirectory().toAbsolutePath();

		// Copy all the files there
		for (StagingObject stageObject : manifest) {
			if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				if (!sourcePath.isAbsolute()) {
					sourcePath = sourceDirectory.resolve(object.getSourcePath()).toAbsolutePath();
				}
				Path targetPath = targetDirectory.resolve(object.getTargetPath());

				jobLogger.info("Copying from " + sourcePath + " to " + targetPath);
				String copyId = sourceFileSystem.copy(sourcePath, targetFileSystem, targetPath, CopyMode.REPLACE,
						false);
				waitForCopy(copyId, sourceFileSystem, manifest, stageObject);
			} else if (stageObject instanceof StringToFileStagingObject) {
				StringToFileStagingObject object = (StringToFileStagingObject) stageObject;
				Path targetPath = targetDirectory.resolve(object.getTargetPath());
				jobLogger.info("Writing string to: " + targetPath);
				
				String contents = object.getSourceString();
				
				jobLogger.debug("Input contents: " + contents);
				
				PrintWriter out = new PrintWriter(targetFileSystem.writeToFile(targetPath));
				out.write(contents);
				out.close();
				
				stageObject.setBytesCopied(contents.length());
			} else if (stageObject instanceof DirectoryStagingObject) {
				DirectoryStagingObject object = (DirectoryStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				if (!sourcePath.isAbsolute()) {
					sourcePath = sourceDirectory.resolve(object.getSourcePath()).toAbsolutePath();
				}
				Path targetPath = targetDirectory.resolve(object.getTargetPath());

				jobLogger.info("Copying from " + sourcePath + " to " + targetPath);
				String copyId = sourceFileSystem.copy(sourcePath, targetFileSystem, targetPath, CopyMode.REPLACE,
						true);
				waitForCopy(copyId, sourceFileSystem, manifest, stageObject);
			}
		}
	}

	private void waitForCopy(String copyId, FileSystem sourceFileSystem, StagingManifest manifest, StagingObject stageObject) throws XenonException, StatePreconditionException {
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
		
		stageObject.setBytesCopied(s.bytesCopied());
	}

	/**
	 * Do the staging from local to remote
	 * 
	 * @param manifest
	 * @return 
	 * @throws StatePreconditionException 
	 * @throws XenonException 
	 */
	public void stageIn(StagingManifest manifest) throws XenonException, StatePreconditionException {
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
	 * @return 
	 * @throws StatePreconditionException 
	 * @throws XenonException 
	 * @throws IOException 
	 */
	public WorkflowBinding stageOut(StagingManifest manifest) throws StatePreconditionException, IOException, XenonException {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + manifest.getJobId());

		try {
			doStaging(manifest, remoteFileSystem, localFileSystem);
		} catch (XenonException e) {
			jobService.setErrorAndState(manifest.getJobId(), e, JobState.STAGING_OUT, JobState.PERMANENT_FAILURE);
			return null;
		}

		Job job = repository.findOne(manifest.getJobId());
		WorkflowBinding binding = job.getOutput();
		for (StagingObject stageObject : manifest) {
			String outputTarget = null;
			Object outputObject = null;
			if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				UriComponentsBuilder b;
				if (service.getConfig().getTargetFilesystemConfig().isHosted()) {
					b = UriComponentsBuilder.fromUriString(manifest.getBaseurl());
					b.pathSegment(service.getConfig().getTargetFilesystemConfig().getBaseurl());
					b.pathSegment(manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
				} else {
					b = UriComponentsBuilder.fromUriString(localFileSystem.getLocation().toString());
					b.pathSegment(manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
				}
				
				HashMap<String, Object> value = new HashMap<String, Object>();
				value.put("location", b.build().toString());
				value.put("basename", object.getTargetPath().getFileNameAsString());
				value.put("path", manifest.getTargetDirectory().resolve(object.getTargetPath().toString()).toString());
				value.put("class", "File");
				value.put("size", object.getBytesCopied());
				
				outputTarget = object.getTargetPath().getFileNameAsString();
				outputObject = value;
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
				object.setBytesCopied(contents.length());
				
				jobLogger.debug("Read contents from " + sourcePath + ": " + contents);
				outputTarget = object.getTargetString();
				outputObject = contents;
			} else if (stageObject instanceof FileToMapStagingObject) {
				FileToMapStagingObject object = (FileToMapStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				String contents = IOUtils.toString(remoteFileSystem.readFromFile(sourcePath), "UTF-8");
				object.setBytesCopied(contents.length());
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
			binding.put(outputTarget, outputObject);
			//jobService.setOutput(manifest.getJobId(), outputTarget, outputObject);
		}
		return binding;
		// jobService.setOutputBinding(manifest.getJobId(), binding);
	}
}

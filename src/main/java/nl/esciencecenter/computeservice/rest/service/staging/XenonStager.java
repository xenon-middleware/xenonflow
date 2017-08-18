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
import nl.esciencecenter.computeservice.rest.model.Job.StateEnum;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.CopyMode;
import nl.esciencecenter.xenon.filesystems.CopyStatus;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class XenonStager {
	private FileSystem localFileSystem;
	private FileSystem remoteFileSystem;
	private JobRepository repository;

	public XenonStager(JobRepository repository, FileSystem localFileSystem, FileSystem remoteFileSystem) {
		super();
		this.localFileSystem = localFileSystem;
		this.remoteFileSystem = remoteFileSystem;
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
	 * @throws XenonException
	 */
	public Job doStaging(StagingManifest manifest, FileSystem sourceFileSystem, FileSystem targetFileSystem)
			throws XenonException {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + manifest.getJobId());
		Job job = repository.findOne(manifest.getJobId());

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
				CopyStatus s = sourceFileSystem.waitUntilDone(copyId, 1000);
				if (s.hasException()) {
					job.setState(StateEnum.SYSTEMERROR);
					job = repository.save(job);
					throw s.getException();
				}
			} else if (stageObject instanceof StringToFileStagingObject) {
				StringToFileStagingObject object = (StringToFileStagingObject) stageObject;
				Path targetPath = targetDirectory.resolve(object.getTargetPath());
				jobLogger.info("Writing string to: " + targetPath);
				PrintWriter out = new PrintWriter(targetFileSystem.writeToFile(targetPath));
				String contents = object.getSourceString();
				jobLogger.debug("Input contents: " + contents);
				out.write(contents);
				out.close();
			}
		}

		return job;
	}

	/**
	 * Do the staging from local to remote
	 * 
	 * @param manifest
	 * @throws XenonException
	 */
	public Job stageIn(StagingManifest manifest) throws XenonException {
		return doStaging(manifest, localFileSystem, remoteFileSystem);
	}

	/**
	 * Do the staging from remote to local and update the job.
	 * 
	 * - FileStagingObject is used to stage files from remote to local -
	 * FileToStringStagingObject is used to read a remote file into a string in
	 * the job output - FileToMapStagingObject is used to read a remote file
	 * into a json object in the job output
	 * 
	 * @param manifest
	 * @throws XenonException
	 * @throws IOException
	 */
	public Job stageOut(StagingManifest manifest) throws XenonException, IOException {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + manifest.getJobId());

		Job job = doStaging(manifest, remoteFileSystem, localFileSystem);

		for (StagingObject stageObject : manifest) {
			if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				UriComponentsBuilder b = UriComponentsBuilder.fromUriString(localFileSystem.getLocation().toString());

				b.pathSegment(manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
				job.getOutput().put(object.getTargetPath().getFileNameAsString(), b.build().toString());
			} else if (stageObject instanceof FileToStringStagingObject) {
				FileToStringStagingObject object = (FileToStringStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				String contents = IOUtils.toString(remoteFileSystem.readFromFile(sourcePath), "UTF-8");
				jobLogger.debug("Read contents from " + sourcePath + ": " + contents);
				job.getOutput().put(object.getTargetString(), contents);
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

					job.getOutput().put(object.getTargetString(), value);
				}
			}
		}

		job = repository.save(job);
		return job;
	}
}

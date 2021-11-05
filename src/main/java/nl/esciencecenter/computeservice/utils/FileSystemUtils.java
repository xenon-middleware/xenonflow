package nl.esciencecenter.computeservice.utils;

import java.io.IOException;

import org.commonwl.cwl.CwlException;
import org.commonwl.cwl.utils.CWLUtils;
import org.commonwl.cwl.utils.CWLUtils.WorkflowDescription;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.XenonflowException;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.computeservice.service.staging.DirectoryStagingObject;
import nl.esciencecenter.computeservice.service.staging.FileStagingObject;
import nl.esciencecenter.computeservice.service.staging.StagingManifest;
import nl.esciencecenter.computeservice.service.staging.StagingManifestFactory;
import nl.esciencecenter.computeservice.service.staging.StagingObject;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class FileSystemUtils {
	public static void deleteRemoteWorkdir(FileSystem remoteFilesystem, Logger jobLogger, Job job) throws XenonException {
		Path remoteDirectory = job.getSandboxDirectory();
		
		if (remoteFilesystem.exists(remoteDirectory)) {
			jobLogger.info("Deleting remote directory: " + remoteDirectory);
			remoteFilesystem.delete(remoteDirectory, true);
		} else {
			jobLogger.info("Remote directory: " + remoteDirectory + " does not exist, skipping.");
		}
	}
	
	public static void deleteInputFromInputDir(XenonService xenonService, Logger jobLogger, Job job) throws XenonException, JsonParseException, JsonMappingException, IOException, CwlException, XenonflowException {
		FileSystem sourceFileSystem = xenonService.getSourceFileSystem();
		FileSystem cwlFileSystem = xenonService.getCwlFileSystem();
		StagingManifest manifest = new StagingManifest(job.getId(), job.getSandboxDirectory());

		WorkflowDescription wfd = CWLUtils.loadLocalWorkflow(job, cwlFileSystem, jobLogger);

		if (wfd.workflow == null || wfd.workflow.getSteps() == null) {
			throw new CwlException("Error staging files, cannot read the workflow file!\nworkflow: " + wfd.workflow);
		}
		
		StagingManifestFactory.addInputToManifest(job, wfd.workflow, manifest, jobLogger);
		
		Path sourceDirectory = sourceFileSystem.getWorkingDirectory().toAbsolutePath();
		for (StagingObject stageObject : manifest) {
			if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				if (!sourcePath.isAbsolute()) {
					sourcePath = sourceDirectory.resolve(object.getSourcePath()).toAbsolutePath();
				}
				
				if (sourceFileSystem.exists(sourcePath)) {
					sourceFileSystem.delete(sourcePath, false);
				}
			} else if (stageObject instanceof DirectoryStagingObject) {
				DirectoryStagingObject object = (DirectoryStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				if (!sourcePath.isAbsolute()) {
					sourcePath = sourceDirectory.resolve(object.getSourcePath()).toAbsolutePath();
				}
				if (sourceFileSystem.exists(sourcePath)) {
					sourceFileSystem.delete(sourcePath, true);
				}
			}
		}
	}
}

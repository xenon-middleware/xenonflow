package nl.esciencecenter.computeservice.utils;

import org.slf4j.Logger;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class RemoteFilesystemUtils {
	public static void deleteRemoteWorkdir(FileSystem remoteFilesystem, Logger jobLogger, Job job) throws XenonException {
		Path remoteDirectory = job.getSandboxDirectory();
		
		if (remoteFilesystem.exists(remoteDirectory)) {
			jobLogger.info("Deleting remote directory: " + remoteDirectory);
			remoteFilesystem.delete(remoteDirectory, true);
		} else {
			jobLogger.info("Remote directory: " + remoteDirectory + " does not exist, skipping.");
		}
	}
}

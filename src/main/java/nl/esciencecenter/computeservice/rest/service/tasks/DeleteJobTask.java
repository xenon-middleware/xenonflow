package nl.esciencecenter.computeservice.rest.service.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class DeleteJobTask implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(DeleteJobTask.class);

	private String jobId;
	private XenonService service;
	private JobRepository repository;

	public DeleteJobTask(String jobId, XenonService service) throws XenonException {
		this.jobId = jobId;
		this.service = service;
		this.repository = service.getRepository();
	}

	@Override
	public void run() {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + jobId);
		Job job = repository.findOne(jobId);
		
		if (job != null) {
			if (!job.getInternalState().isFinal()) {
				jobLogger.info("Waiting for job to be in a final state (probably cancelled)");
				try {
					while (!job.getInternalState().isFinal()) {
						Thread.sleep(1000);
						job = repository.findOne(jobId);
					}
				} catch (InterruptedException e) {
					jobLogger.info("Exception while deleting remote directory:", e);
					logger.error("Exception while deleting remote directory:", e);
				}
			}
			
			try {
				// delete remote directory
				Path remoteDirectory = job.getSandboxDirectory();
				FileSystem remoteFilesystem = service.getRemoteFileSystem();
				
				if (remoteFilesystem.exists(remoteDirectory)) {
					jobLogger.info("Deleting remote directory: " + remoteDirectory);
					remoteFilesystem.delete(remoteDirectory, true);
				} else {
					jobLogger.info("Remote directory: " + remoteDirectory + " does not exist, skipping.");
				}
			} catch (XenonException e) {
				jobLogger.info("Exception while deleting remote directory:", e);
				logger.error("Exception while deleting remote directory:", e);
			}
			
			try {
				// delete local output directory if it exists
				FileSystem localFilesystem = service.getSourceFileSystem();
				Path localDirectory = localFilesystem.getWorkingDirectory().resolve("out/" + job.getId() + "/").toAbsolutePath();
				
				if (localFilesystem.exists(localDirectory)) {
					jobLogger.info("Deleting local output directory: " + localDirectory);
					localFilesystem.delete(localDirectory, true);
				} else {
					jobLogger.info("Local directory: " + localFilesystem + " does not exist, skipping.");
				}
			} catch (XenonException e) {
				jobLogger.info("Exception while deleting remote directory:", e);
				logger.error("Exception while deleting remote directory:", e);
			}

			// delete the job
			jobLogger.info("Deleting job " + jobId);
			logger.info("Deleting job " + jobId);
			repository.delete(jobId);
			
		}
	}

}

package nl.esciencecenter.computeservice.service.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.schedulers.JobStatus;
import nl.esciencecenter.xenon.schedulers.Scheduler;

@Component
public class DeleteJobTask {
	private static final Logger logger = LoggerFactory.getLogger(DeleteJobTask.class);

	@Autowired
	private XenonService xenonService;
	
	@Autowired
	private JobRepository repository;

	public void deleteJob(String jobId) {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + jobId);
		Job job = repository.findById(jobId).get();
		
		if (job != null) {
			// Once we are in this function the staging has already been cancelled because of
			// the order of execution in XenonMonitoringTask
			// It can have any state, but we ignore it all and just cancel and delete everything
			// afterwards the job will no longer exist.
			try {				
				// cancel the job if it's running.
				if (job.getInternalState().isRemote()) {
					String xenonJobId = job.getXenonId();
					if (xenonJobId != null && !xenonJobId.isEmpty()) {
						Scheduler scheduler = xenonService.getScheduler();
						JobStatus status = scheduler.getJobStatus(xenonJobId);

						if (status.isRunning()) {
							status = scheduler.cancelJob(job.getXenonId());
							logger.debug("Cancelled job: " + job.getId() + " new status: " + status);
						}
					}
				}
			} catch (XenonException e) {
				jobLogger.info("Exception while cancelling job:", e);
				logger.error("Exception while concelling job:", e);
			}
			
			try {
				// delete remote directory
				Path remoteDirectory = job.getSandboxDirectory();
				FileSystem remoteFilesystem = xenonService.getRemoteFileSystem();
				
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
				FileSystem localFilesystem = xenonService.getSourceFileSystem();
				Path localDirectory = localFilesystem.getWorkingDirectory().resolve("out/" + job.getId() + "/").toAbsolutePath();
				
				if (localFilesystem.exists(localDirectory)) {
					jobLogger.info("Deleting local output directory: " + localDirectory);
					localFilesystem.delete(localDirectory, true);
				} else {
					jobLogger.info("Local directory: " + localFilesystem + " does not exist, skipping.");
				}
			} catch (XenonException e) {
				jobLogger.info("Exception while deleting local directory:", e);
				logger.error("Exception while deleting local directory:", e);
			}

			// delete the job
			jobLogger.info("Deleting job " + jobId);
			logger.info("Deleting job " + jobId);
			repository.deleteById(jobId);	
		}
	}

}

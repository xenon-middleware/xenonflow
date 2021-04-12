package nl.esciencecenter.computeservice.service.tasks;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.model.JobState;
import nl.esciencecenter.computeservice.model.StatePreconditionException;
import nl.esciencecenter.computeservice.service.JobService;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.adaptors.NotConnectedException;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.schedulers.Scheduler;

public class CwlWorkflowTask implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(CwlWorkflowTask.class);

	private String jobId;
	
	private XenonService service;
	private JobRepository repository;
	private JobService jobService;

	public CwlWorkflowTask(String jobId, XenonService service) throws XenonException {
		this.jobId = jobId;
		this.service = service;
		this.repository = service.getRepository();
		this.jobService = service.getJobService();
	}

	@Override
	public void run() {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + jobId);
		Job job = repository.findById(jobId).get();
		try {
			Scheduler scheduler = service.getScheduler();

			job = repository.findById(jobId).get();

			if (job.getInternalState().isCancellationActive()) {
				jobService.setJobState(jobId, job.getInternalState(), JobState.CANCELLED);
				return;
			}

			if (job.getInternalState().isFinal()) {
				return;
			}

			Path remoteDirectory = job.getSandboxDirectory();

			// Create a new job description
			nl.esciencecenter.xenon.schedulers.JobDescription description = new nl.esciencecenter.xenon.schedulers.JobDescription();
			description.setExecutable("./cwlcommand");

			ArrayList<String> cwlArguments = new ArrayList<String>();
			cwlArguments.add(new Path(job.getWorkflow()).getFileNameAsString());

			if (job.hasInput()) {
				cwlArguments.add("job-order.json");
			}

			description.setArguments(cwlArguments.toArray(new String[cwlArguments.size()]));
			description.setStdout("stdout.txt");
			description.setStderr("stderr.txt");
			
			int maxTimeMinutes = service.getConfig().defaultComputeResource().getMaxTime();
			jobLogger.debug("Setting maximum running time to: " + maxTimeMinutes);
			description.setMaxRuntime(maxTimeMinutes);

			jobLogger.debug("Setting remote working directory to: " + remoteDirectory);
			description.setWorkingDirectory(remoteDirectory.toString());
			
			jobLogger.debug("Executing description: " + description);

			jobLogger.debug("Submitting the job: " + description);

			String xenonJobId = null;
			int tries = 0;
			while(xenonJobId == null && tries < 3) {
				try {
					tries++;
					xenonJobId = scheduler.submitBatchJob(description);
				} catch (NotConnectedException e) {
					if (tries <=3 ) {
						logger.warn("Try: " + tries + ". Exception during job submission, forcing new scheduler for next attempt");
						scheduler = service.forceNewScheduler();
					} else {
						logger.error("Failed to submit after " + tries + " tries, giving up");
					}
					continue;
				}
			}
			
			if (xenonJobId != null) {
				jobLogger.info("Xenon jobid: " + xenonJobId);
	
				jobService.setXenonJobId(jobId, xenonJobId);			
				jobService.setJobState(jobId, JobState.XENON_SUBMIT, JobState.WAITING);
			} else {
				jobService.setJobState(jobId, JobState.XENON_SUBMIT, JobState.SYSTEM_ERROR);
			}
		} catch (StatePreconditionException | XenonException e) {
			try {
				jobService.setJobState(jobId, JobState.XENON_SUBMIT, JobState.SYSTEM_ERROR);
			} catch (StatePreconditionException e1) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e1);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e1);
			}
			jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
			logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
		}
	}

}

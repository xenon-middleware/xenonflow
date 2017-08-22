package nl.esciencecenter.computeservice.rest.service.tasks;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.model.JobState;
import nl.esciencecenter.computeservice.rest.service.JobService;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.xenon.XenonException;
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
		Job job = repository.findOne(jobId);
		try {
			Scheduler scheduler = service.getScheduler();

			job = repository.findOne(jobId);

			if (job.getInternalState().isCancellationActive()) {
				jobService.setJobState(jobId, job.getInternalState(), JobState.CANCELLED);
				return;
			}

			if (job.getInternalState().isFinal()) {
				return;
			}

			Path remoteDirectory = service.getRemoteFileSystem().getWorkingDirectory()
					.resolve(job.getSandboxDirectory());

			// Create a new job description
			nl.esciencecenter.xenon.schedulers.JobDescription description = new nl.esciencecenter.xenon.schedulers.JobDescription();
			description.setExecutable(service.getConfig().getCwlCommand());

			jobLogger.debug("Using cwl command: " + description.getExecutable());

			ArrayList<String> cwlArguments = new ArrayList<String>();
			cwlArguments.add(remoteDirectory.resolve(new Path(job.getWorkflow()).getFileName()).toString());

			if (job.hasInput()) {
				cwlArguments.add(remoteDirectory.resolve("job-order.json").toString());
			}

			description.setArguments(cwlArguments.toArray(new String[cwlArguments.size()]));
			description.setStdout(remoteDirectory.resolve("stdout.txt").toString());
			description.setStderr(remoteDirectory.resolve("stderr.txt").toString());
			description.setWorkingDirectory(remoteDirectory.toString());

			jobLogger.debug("Submitting the job: " + description);
			String xenonJobId = scheduler.submitBatchJob(description);
			jobLogger.info("Xenon jobid: " + xenonJobId);

			jobService.setXenonJobId(jobId, xenonJobId);			
			jobService.setJobState(jobId, JobState.READY, JobState.WAITING);
		} catch (XenonException e) {
			jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
			logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

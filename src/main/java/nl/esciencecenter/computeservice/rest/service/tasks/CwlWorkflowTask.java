package nl.esciencecenter.computeservice.rest.service.tasks;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.Job.InternalStateEnum;
import nl.esciencecenter.computeservice.rest.model.Job.StateEnum;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.schedulers.JobStatus;
import nl.esciencecenter.xenon.schedulers.Scheduler;

public class CwlWorkflowTask implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(CwlWorkflowTask.class);

	private String jobId;
	private XenonService service;
	private JobRepository repository;

	public CwlWorkflowTask(String jobId, XenonService service) throws XenonException {
		this.jobId = jobId;
		this.service = service;
		this.repository = service.getRepository();
	}

	@Override
	public void run() {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + jobId);
		Job job = repository.findOne(jobId);
		try {
			Scheduler scheduler = service.getScheduler();

			// Job has changed during StageIn so get it from the database again.
			job = repository.findOne(jobId);
			if (job.isDone()) {
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

			job.setXenonId(xenonJobId);
			job.getAdditionalInfo().put("xenon.id", xenonJobId);
			job = repository.save(job);

			JobStatus status = scheduler.waitUntilRunning(xenonJobId, 0);

			// Unless there was an exception we will assume it is running
			// this is later checked in the XenonWaitingTask if there
			// was any other error during execution it is handled
			// there.
			job.setState(StateEnum.RUNNING);
			job.setInternalState(InternalStateEnum.RUNNING);
			job.getAdditionalInfo().put("xenon.state", status.getState());

			if (status.hasException()) {
				jobLogger.error("Exception during execution", status.getException());
				job.setState(StateEnum.PERMANENTFAILURE);
				job.setInternalState(InternalStateEnum.ERROR);
				job.getAdditionalInfo().put("xenon.error", status.getException());
			}

			job = repository.save(job);
		} catch (XenonException e) {
			jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
			logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
		}
	}

}

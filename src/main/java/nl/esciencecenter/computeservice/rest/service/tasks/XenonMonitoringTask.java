package nl.esciencecenter.computeservice.rest.service.tasks;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.Job.InternalStateEnum;
import nl.esciencecenter.computeservice.rest.model.Job.StateEnum;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.adaptors.schedulers.JobCanceledException;
import nl.esciencecenter.xenon.schedulers.JobStatus;
import nl.esciencecenter.xenon.schedulers.NoSuchJobException;
import nl.esciencecenter.xenon.schedulers.Scheduler;

public class XenonMonitoringTask implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(XenonMonitoringTask.class);

	private XenonService service;
	private JobRepository repository;

	public XenonMonitoringTask(XenonService service) {
		this.service = service;
		this.repository = service.getRepository();
	}

	@Override
	public void run() {
		List<Job> jobs = repository.findAllByInternalState(InternalStateEnum.RUNNING);
		List<Job> waiting = repository.findAllByInternalState(InternalStateEnum.WAITING);

		// Get the jobs that the user wants us to cancel
		List<Job> cancelled = repository.findAllByStateAndInternalStateNot(StateEnum.CANCELLED, InternalStateEnum.CANCELLED);
		
		List<Job> staged = repository.findAllByInternalState(InternalStateEnum.POST_STAGEIN);

		jobs.addAll(waiting); 
		jobs.addAll(cancelled);
		if (jobs.isEmpty() && staged.isEmpty()) {
			return;
		}

		Scheduler scheduler;
		try {
			scheduler = service.getScheduler();
		} catch (XenonException e) {
			logger.error("Error getting the xenon scheduler", e);;
			return;
		}
		
		for (Job job: staged) {
			Logger jobLogger = LoggerFactory.getLogger("jobs."+job.getId());
			job.setInternalState(InternalStateEnum.SUBMITTING);
    		job = repository.save(job);
			try {
				service.getTaskScheduler().execute(new CwlWorkflowTask(job.getId(), service));
			} catch (XenonException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
				logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
			}
		}

		for (Job job : jobs) {
			Logger jobLogger = LoggerFactory.getLogger("jobs."+job.getId());
			try {
				String xenonJobId = job.getXenonId();
				if (xenonJobId != null && !xenonJobId.isEmpty()) {
					JobStatus status = scheduler.getJobStatus(xenonJobId);
		            job.getAdditionalInfo().put("xenon.state", status.getState());
			        
		        	if (job.isRunningOrWaiting() && status.isRunning()) {
		        		job.setState(StateEnum.RUNNING);
		        		job.setInternalState(InternalStateEnum.RUNNING);
		        		
		        		job = repository.save(job);
		        	} else if (status.isDone()) {
			        	job.getAdditionalInfo().put("xenon.exitcode", status.getExitCode());
			        
			        	if (status.hasException() && !job.isCancelled()) {
			        		jobLogger.error("Exception during execution", status.getException());
			        		job.setState(StateEnum.PERMANENTFAILURE);
			        		job.setInternalState(InternalStateEnum.DONE);
			        		job.getAdditionalInfo().put("xenon.error", status.getException());
			        	} else if (status.hasException() && job.isCancelled() && job.getInternalState() != InternalStateEnum.CANCELLED){
			        		jobLogger.error("Job was cancelled", status.getException());
			        		job.getAdditionalInfo().put("xenon.error", status.getException());
			        		job.setInternalState(InternalStateEnum.CANCELLED);
			        	} else if (status.getExitCode() != 0) {
			        		jobLogger.error("Job has finished with errors.");
			        		job.setState(StateEnum.PERMANENTFAILURE);
			        		job.setInternalState(InternalStateEnum.DONE);
			        	} else {
			        		jobLogger.info("Jobs done.");
			        		job.setState(StateEnum.SUCCESS);
			        		job.setInternalState(InternalStateEnum.DONE);
			        	}
			        	
			        	job = repository.save(job);
			        	
			        	if (job.stageBackPossible()) {
			        		job.setInternalState(InternalStateEnum.STAGEOUT);
			        		job = repository.save(job);
			        		service.getTaskScheduler().execute(new CwlStageOutTask(job.getId(), status, service));
			        	}
			        }
				}
			} catch (NoSuchJobException e) {
				if (!job.isCancelled()) {
					logger.info("Could not recover job" + job + " it is probably lost...");
					// TODO: We should probably try harder here to figure out what went wrong
					// in additional info there may be a xenon.state. Plus we could try
					// staging back the stdout and stderr of this job.
					job.setState(StateEnum.SYSTEMERROR);
					job.setInternalState(InternalStateEnum.DONE);
					job = repository.save(job);
				}
			} catch (JobCanceledException e) {
				if (job.getInternalState() != InternalStateEnum.CANCELLED) {
					jobLogger.error("Job " + job.getName() + "(" +job.getId() +") was unexpectedly cancelled", e);
					logger.error("Job " + job.getName() + "(" +job.getId() +") was unexpectedly cancelled", e);;
				} else {
					// Set the Xenon state here, because they decided to throw an error instead
					// of returning a Cancelled state. So we have to come up with our own string.
					job.getAdditionalInfo().put("xenon.state", "CANCELLED");
				}
			} catch (XenonException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
				logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
			}
		}
		
	}

}

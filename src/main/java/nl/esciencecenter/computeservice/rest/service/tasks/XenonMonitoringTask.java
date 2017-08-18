package nl.esciencecenter.computeservice.rest.service.tasks;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.Job.StateEnum;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.xenon.XenonException;
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
		List<Job> jobs = repository.findAllByState(StateEnum.RUNNING);
		List<Job> waiting = repository.findAllByState(StateEnum.WAITING);

		jobs.addAll(waiting); 
		if (jobs.isEmpty()) {
			return;
		}

		Scheduler scheduler;
		try {
			scheduler = service.getScheduler();
		} catch (XenonException e) {
			logger.error("Error getting the xenon scheduler", e);;
			return;
		}

		for (Job job : jobs) {      
			Logger jobLogger = LoggerFactory.getLogger("jobs."+job.getId());
			try {
				JobStatus status = scheduler.getJobStatus(job.getXenonId());
	            job.getAdditionalInfo().put("xenon.state", status.getState());
		        
	        	if (job.getState() == StateEnum.WAITING && status.isRunning()) {
	        		job.setState(StateEnum.RUNNING);
	        		
	        		job = repository.save(job);
	        	} else if (status.isDone()) {
		        	job.getAdditionalInfo().put("xenon.exitcode", status.getExitCode());
		        
		        	if (status.hasException()) {
		        		jobLogger.error("Exception during execution", status.getException());
		        		job.setState(StateEnum.PERMANENTFAILURE);
		        		job.getAdditionalInfo().put("xenon.error", status.getException());
		        		job = repository.save(job);
		        		continue;
		        	}
		        
		        	if (status.getExitCode() != 0) {
		        		jobLogger.error("Job has finished with errors.");
		        		job.setState(StateEnum.PERMANENTFAILURE);
		        	} else {
		        		jobLogger.info("Jobs done.");
		        		job.setState(StateEnum.SUCCESS);
		        	}
		        	
		        	job = repository.save(job);
		        	service.getTaskScheduler().execute(new CwlStageOutTask(job.getId(), status, service));
		        }
			} catch (NoSuchJobException e) {
				logger.info("Could not recover job" + job + " it is probably lost...");
				// TODO: We should probably try harder here to figure out what went wrong
				// in additional info there may be a xenon.state. Plus we could try
				// staging back the stdout and stderr of this job.
				job.setState(StateEnum.SYSTEMERROR);
				job = repository.save(job);
			} catch (XenonException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
				logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
				return;
			}
		}
		
	}

}

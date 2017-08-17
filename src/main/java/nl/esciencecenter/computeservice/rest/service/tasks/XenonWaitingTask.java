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
import nl.esciencecenter.xenon.schedulers.Scheduler;

public class XenonWaitingTask implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(XenonWaitingTask.class);

	private XenonService service;
	private JobRepository repository;

	public XenonWaitingTask(XenonService service) {
		this.service = service;
		this.repository = service.getRepository();
	}

	@Override
	public void run() {
		List<Job> jobs = repository.findAllByState(StateEnum.RUNNING);
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
	        	job = repository.save(job);
		        
	        	if (status.isDone()) {
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
		        		job = repository.save(job);
		        	} else {
		        		jobLogger.info("Jobs done.");
		        		job.setState(StateEnum.SUCCESS);
		        		job = repository.save(job);
		        	}
		        	service.getTaskScheduler().execute(new CwlStageOutTask(job.getId(), status, service));
		        }
			} catch (XenonException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
				logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
				return;
			}
		}
		
	}

}

package nl.esciencecenter.computeservice.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.model.JobState;
import nl.esciencecenter.computeservice.model.StatePreconditionException;
import nl.esciencecenter.computeservice.model.WorkflowBinding;
import nl.esciencecenter.xenon.filesystems.Path;

@Service
public class JobService {
	private static final Logger logger = LoggerFactory.getLogger(JobService.class);
	
	@Autowired
	private JobRepository repository;
	
	@Transactional
	public Job setJobState(String jobId, JobState from, JobState to) throws StatePreconditionException {
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
		
		Job job = repository.findOneForUpdate(jobId);
		job.changeState(from, to);
		
		if (to.isFinal()) {
			job.getAdditionalInfo().put("finalizedAt", new Date());
		}
		
        job = repository.saveAndFlush(job);
        
        jobLogger.info("Job " + jobId + " now has state: " + to);
        return job;
	}
	
	@Transactional
	public void setErrorAndState(String jobId, Exception e, JobState from, JobState to) {
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
	
		try {
			Job job = repository.findOneForUpdate(jobId);

			if (e != null) {
				jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
				logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
				job.getAdditionalInfo().put("error", e);
				job = repository.saveAndFlush(job);
			}
			setJobState(jobId, from, to);
		} catch (StatePreconditionException except) {
			logger.error("Error during update of Job (" +jobId +")", except);
		}
	}
	
	@Transactional
	public void setXenonRemoteDir(String jobId, Path remoteDirectory){
		Job job = repository.findOneForUpdate(jobId);
        job.getAdditionalInfo().put("xenon.remote.directory", remoteDirectory.toString());
        job = repository.saveAndFlush(job);
	}

	@Transactional
	public void setOutput(String jobId, String outputTarget, Object outputObject) {
		Job job = repository.findOneForUpdate(jobId);
    	job.getOutput().put(outputTarget, outputObject);
    	job = repository.saveAndFlush(job);
	}

	@Transactional
	public void setXenonJobId(String jobId, String xenonJobId) {
		Job job = repository.findOneForUpdate(jobId);
		job.setXenonId(xenonJobId);
		job.getAdditionalInfo().put("xenon.id", xenonJobId);
		job = repository.saveAndFlush(job);
	}

	@Transactional
	public void setXenonState(String jobId, String state) {
		Job job = repository.findOneForUpdate(jobId);
		job.getAdditionalInfo().put("xenon.state", state);
		job = repository.saveAndFlush(job);
	}

	@Transactional
	public void setXenonExitcode(String jobId, Integer exitCode) {
		Job job = repository.findOneForUpdate(jobId);
		job.getAdditionalInfo().put("xenon.exitcode", exitCode);
		job = repository.saveAndFlush(job);
	}

	@Transactional
	public Job setOutputBinding(String jobId, WorkflowBinding binding) {    	
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
		
		Job job = repository.findOneForUpdate(jobId);
		job.setOutput(binding);
		
	
        job = repository.saveAndFlush(job);
        
        jobLogger.info("Job " + jobId + " now has ouptut: " + job.getOutput());
        return job;    	
    	
	}
	
	@Transactional
	public Job setInput(String jobId, WorkflowBinding binding) {    	
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
		
		Job job = repository.findOneForUpdate(jobId);
		job.setInput(binding);
		
	
        job = repository.saveAndFlush(job);
        
        jobLogger.info("Job " + jobId + " now has input: " + job.getInput());
        return job;    	
    	
	}

	@Transactional
	public void setAdditionalInfo(String jobId, String key, Object info) {
		Job job = repository.findOneForUpdate(jobId);
		job.getAdditionalInfo().put(key, info);
		job = repository.saveAndFlush(job);
	}

	@Transactional
	public Job completeJob(String jobId, WorkflowBinding files, JobState from, JobState to) throws StatePreconditionException {
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
		
		Job job = repository.findOneForUpdate(jobId);
		job.changeState(from, to);
		
		if (files != null) {
			job.setOutput(files);
		}
		if (to.isFinal()) {
			job.getAdditionalInfo().put("finalizedAt", new Date());
		}
		
        job = repository.saveAndFlush(job);
        
        jobLogger.info("Job " + jobId + " now has state: " + to);
        return job;
	}
}

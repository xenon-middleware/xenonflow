package nl.esciencecenter.computeservice.rest.service;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.model.JobState;
import nl.esciencecenter.xenon.filesystems.Path;

@Service
public class JobService {
	private static final Logger logger = LoggerFactory.getLogger(JobService.class);
	
	@Autowired
	JobRepository repository;
	
	@Transactional
	public Job setJobState(String jobId, JobState from, JobState to) throws Exception {
		Job job = repository.findOneForUpdate(jobId);
		job.changeState(from, to);
        job = repository.save(job);
        return job;
	}
	
	@Transactional
	public void setErrorAndState(String jobId, Exception e, JobState from, JobState to) {
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
	
		try {
			Job job = repository.findOneForUpdate(jobId);

			jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
		
			setJobState(jobId, from, to);

			job.getAdditionalInfo().put("error", e);
			job = repository.save(job);
		} catch (Exception except) {
			logger.error("Error during update of Job (" +jobId +")", except);
		}
	}
	
	@Transactional
	public void setXenonRemoteDir(String jobId, Path remoteDirectory){
		Job job = repository.findOneForUpdate(jobId);
        job.getAdditionalInfo().put("xenon.remote.directory", remoteDirectory.toString());
        job = repository.save(job);
	}
	
	@Transactional
	public void setOutput(String jobId, String outputTarget, Object outputObject) {
		Job job = repository.findOneForUpdate(jobId);
    	job.getOutput().put(outputTarget, outputObject);
    	repository.save(job);
	}

	@Transactional
	public void setXenonJobId(String jobId, String xenonJobId) {
		Job job = repository.findOneForUpdate(jobId);
		job.setXenonId(xenonJobId);
		job.getAdditionalInfo().put("xenon.id", xenonJobId);
		repository.save(job);
	}

	@Transactional
	public void setXenonState(String jobId, String state) {
		Job job = repository.findOneForUpdate(jobId);
		job.getAdditionalInfo().put("xenon.state", state);
		repository.save(job);
	}

	@Transactional
	public void setXenonExitcode(String jobId, Integer exitCode) {
		Job job = repository.findOneForUpdate(jobId);
		job.getAdditionalInfo().put("xenon.exitcode", exitCode);
		repository.save(job);
	}
}

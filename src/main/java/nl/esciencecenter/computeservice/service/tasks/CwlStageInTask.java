package nl.esciencecenter.computeservice.service.tasks;

import java.io.IOException;
import java.util.Optional;

import org.commonwl.cwl.CwlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.model.JobState;
import nl.esciencecenter.computeservice.model.StatePreconditionException;
import nl.esciencecenter.computeservice.model.XenonflowException;
import nl.esciencecenter.computeservice.service.JobService;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.computeservice.service.staging.StagingManifest;
import nl.esciencecenter.computeservice.service.staging.StagingManifestFactory;
import nl.esciencecenter.computeservice.service.staging.XenonStager;
import nl.esciencecenter.xenon.XenonException;

public class CwlStageInTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(CwlStageInTask.class);
	
	private String jobId;
	
	private XenonService service;	
	private JobRepository repository;
	private JobService jobService;
	private XenonStager sourceToRemoteStager;

	public CwlStageInTask(String jobId, XenonStager sourceToRemoteStager, XenonService service) throws XenonException {
		this.jobId = jobId;
		this.service = service;
		this.sourceToRemoteStager = sourceToRemoteStager;
		this.repository = service.getRepository();
		this.jobService = service.getJobService();
	}
	
	@Override
	public void run(){
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
		try {
			Optional<Job> j = repository.findById(jobId);
			
			if (!j.isPresent()) {
				logger.error("Could not find job with id: " + jobId);
				jobLogger.error("Could not find job with id: " + jobId);
				return;
			}
			
			Job job = j.get();
			if (job.getInternalState().isFinal()) {
				// The job is in a final state so it's likely failed
				// or cancelled.
				return;
			}
			
			if (job.getInternalState() == JobState.SUBMITTED){
				job = jobService.setJobState(jobId, JobState.SUBMITTED, JobState.STAGING_IN);
			} else if (job.getInternalState() != JobState.STAGING_IN) {
				throw new StatePreconditionException("State is: " + job.getInternalState() + " but expected either SUBMITTED or STAGING_IN");
			}
			
			
			String cwlCommand = service.getConfig().defaultComputeResource().getCwlCommand();
			// Staging files
			StagingManifest manifest = StagingManifestFactory.createStagingInManifest(job, service.getCwlFileSystem(), service.getSourceFileSystem(), cwlCommand, jobLogger, jobService);
	       
			sourceToRemoteStager.stageIn(manifest);
	        
	        jobService.setXenonRemoteDir(jobId, service.getRemoteFileSystem().getWorkingDirectory().resolve(manifest.getTargetDirectory()));
		} catch (CwlException | StatePreconditionException e) {
			jobLogger.error("Error during stage-in: ", e);
			logger.error("Error during stage-in: ", e);
			jobService.setErrorAndState(jobId, e, JobState.STAGING_IN, JobState.SYSTEM_ERROR);
		} catch (XenonException | XenonflowException | IOException e) {
			jobLogger.error("Error during stage-in: ", e);
			logger.error("Error during stage-in: ", e);
			jobService.setErrorAndState(jobId, e, JobState.STAGING_IN, JobState.PERMANENT_FAILURE);
		}
	}
}

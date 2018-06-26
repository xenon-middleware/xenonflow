package nl.esciencecenter.computeservice.rest.service.tasks;

import java.io.IOException;

import org.commonwl.cwl.CwlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.model.JobState;
import nl.esciencecenter.computeservice.rest.model.StatePreconditionException;
import nl.esciencecenter.computeservice.rest.model.XenonflowException;
import nl.esciencecenter.computeservice.rest.service.JobService;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.computeservice.rest.service.staging.StagingManifest;
import nl.esciencecenter.computeservice.rest.service.staging.StagingManifestFactory;
import nl.esciencecenter.computeservice.rest.service.staging.XenonStager;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.adaptors.NotConnectedException;

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
			Job job = repository.findOne(jobId);
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
			
			// Staging files
			StagingManifest manifest = StagingManifestFactory.createStagingManifest(job, service.getSourceFileSystem(), jobLogger);
	        
			int tries = 0;
			boolean success = false;
			while(!success && tries < 3) {
				try {
					success = sourceToRemoteStager.stageIn(manifest);
					tries++;
				} catch (NotConnectedException e) {
					if (tries <=3 ) {
						logger.warn("Try: " + tries + ". Exception during stage in, forcing new filesystem for next attempt");
						sourceToRemoteStager.setFileSystems(service.getSourceFileSystem(), service.getRemoteFileSystem());
					} else {
						logger.error("Failed to submit after " + tries + " tries, giving up");
					}
					continue;
				}
			}
	        
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

package nl.esciencecenter.computeservice.service.tasks;

import java.io.IOException;

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
import nl.esciencecenter.xenon.adaptors.NotConnectedException;

public class CwlStageOutTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(CwlStageOutTask.class);
	
	private String jobId;
	private Integer exitcode;
	private XenonService service;
	private JobRepository repository;
	private JobService jobService;
	private XenonStager remoteToTargetStager;

	public CwlStageOutTask(String jobId, Integer exitcode, XenonStager remoteToTargetStager, XenonService service) throws XenonException {
		this.jobId = jobId;
		this.service = service;
		this.remoteToTargetStager = remoteToTargetStager;
		this.repository = service.getRepository();
		this.jobService = service.getJobService();
		this.exitcode = exitcode;
	}
	
	@Override
	public void run() {
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
		Job job = repository.findById(jobId).get();
		if (job.getInternalState().isFinal()) {
			// The job is in a final state so it's likely failed
			// or cancelled.
			return;
		}

		try {
			
			if (job.getInternalState() == JobState.FINISHED){
				job = jobService.setJobState(jobId, JobState.FINISHED, JobState.STAGING_OUT);
			} else if (job.getInternalState() != JobState.STAGING_OUT) {
				throw new StatePreconditionException("State is: " + job.getInternalState() + " but expected either FINISHED or STAGING_OUT");
			}

			// Staging back output
	        StagingManifest manifest = StagingManifestFactory.createStagingOutManifest(job, exitcode, service.getSourceFileSystem(), service.getRemoteFileSystem(), jobService, jobLogger); //new StagingManifest(jobId, new Path(job.getId() + "/"));

	    	int tries = 0;
			boolean success = false;
			while(!success && tries < 3) {
				try {
					success = remoteToTargetStager.stageOut(manifest, exitcode);
					tries++;
				} catch (NotConnectedException e) {
					if (tries <=3 ) {
						logger.warn("Try: " + tries + ". Exception during stage out, forcing new filesystem for next attempt");
						remoteToTargetStager.setFileSystems(service.getTargetFileSystem(), service.getRemoteFileSystem());
					} else {
						logger.error("Failed to submit after " + tries + " tries, giving up");
					}
					continue;
				}
			}
		} catch (StatePreconditionException | IOException | XenonException | XenonflowException | CwlException e){
			jobLogger.error("Error during stage out of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during stage out of " + job.getName() + "(" +job.getId() +")", e);
		}
	}
}

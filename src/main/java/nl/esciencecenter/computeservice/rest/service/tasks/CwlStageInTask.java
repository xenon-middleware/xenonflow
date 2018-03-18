package nl.esciencecenter.computeservice.rest.service.tasks;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import org.apache.commons.io.FilenameUtils;
import org.commonwl.cwl.CwlException;
import org.commonwl.cwl.InputParameter;
import org.commonwl.cwl.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.model.JobState;
import nl.esciencecenter.computeservice.rest.model.StatePreconditionException;
import nl.esciencecenter.computeservice.rest.service.JobService;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.computeservice.rest.service.staging.DirectoryStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.FileStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.StagingManifest;
import nl.esciencecenter.computeservice.rest.service.staging.StagingManifestFactory;
import nl.esciencecenter.computeservice.rest.service.staging.StringToFileStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.XenonStager;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class CwlStageInTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(CwlStageInTask.class);
	
	private String jobId;
	private XenonService service;
	private JobRepository repository;
	private JobService jobService;

	public CwlStageInTask(String jobId, XenonService service) throws XenonException {
		this.jobId = jobId;
		this.service = service;
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
			
			XenonStager stager = new XenonStager(jobService, repository, service.getSourceFileSystem(), service.getRemoteFileSystem(), service);

			// Staging files
			StagingManifest manifest = StagingManifestFactory.createStagingManifest(job, service.getSourceFileSystem(), jobLogger);
	        
	        stager.stageIn(manifest);
	        
	        jobService.setXenonRemoteDir(jobId, service.getRemoteFileSystem().getWorkingDirectory().resolve(manifest.getTargetDirectory()));
	        jobService.setJobState(jobId, JobState.STAGING_IN, JobState.STAGING_READY);

	        jobLogger.info("StageIn complete.");
		} catch (CwlException | StatePreconditionException e) {
			jobLogger.error("Error during stage-in: ", e);
			logger.error("Error during stage-in: ", e);
			jobService.setErrorAndState(jobId, e, JobState.STAGING_IN, JobState.SYSTEM_ERROR);
		} catch (XenonException | IOException e) {
			jobLogger.error("Error during stage-in: ", e);
			logger.error("Error during stage-in: ", e);
			jobService.setErrorAndState(jobId, e, JobState.STAGING_IN, JobState.PERMANENT_FAILURE);
		}
	}
}

package nl.esciencecenter.computeservice.rest.service.tasks;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import org.commonwl.cwl.InputParameter;
import org.commonwl.cwl.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
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
				return;
			}
			
			job = jobService.setJobState(jobId, JobState.SUBMITTED, JobState.STAGING_IN);
			
			XenonStager stager = new XenonStager(jobService, repository, service.getSourceFileSystem(), service.getRemoteFileSystem());

			// Staging files
			StagingManifest manifest = new StagingManifest(jobId, job.getSandboxDirectory());

	        // Add the workflow to the staging manifest
	        Path localWorkflow = new Path(job.getWorkflow());
	        Path workflowBaseName = new Path (localWorkflow.getFileNameAsString());
	        
	        // TODO: Recursively go through the workflow to find other cwl files
	        // to stage. Or do we expect ppl to use in-line workflow files
	        manifest.add(new FileStagingObject(localWorkflow, workflowBaseName));
	        
	        Path remoteJobOrder = null;
	        if (job.hasInput()) {
	        	remoteJobOrder = new Path("job-order.json");
	        	
	        	// Add files and directories from the input to the staging
	        	// manifest and update the input to point to locations
	        	// on the remote server
	        	String jobOrderString = job.getInput().toString();
	        	logger.debug("Old job order string: " + jobOrderString);
	        	
	        	// Read in the job order as a hashmap
				ObjectMapper mapper = new ObjectMapper(new JsonFactory());
				TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
				HashMap<String, Object> jobOrder = mapper.readValue(new StringReader(jobOrderString), typeRef);
				
				// Read in the workflow to get the required inputs
				Path workflowPath = service.getSourceFileSystem().getWorkingDirectory().resolve(localWorkflow);
	        	Workflow workflow = Workflow.fromInputStream(service.getSourceFileSystem().readFromFile(workflowPath.toAbsolutePath()));
	        	
	        	for (InputParameter parameter : workflow.getInputs()) {
	        		if (parameter.getType().equals("File")) {
	        			String paramId = null;
	        			if (jobOrder.containsKey(parameter.getId())) {
	        				paramId = parameter.getId();
	        			} else if (parameter.getId().startsWith("#")) {
	        				paramId = parameter.getId().split("/")[1];
	        			}
	        			
        				// This should either work or throw an exception, we know about the problem (type erasure).
        				@SuppressWarnings("unchecked")
						HashMap<String, Object> fileInput = (HashMap<String, Object>) jobOrder.get(paramId);

        				Path localPath = new Path((String) fileInput.get("path"));
        				Path remotePath = new Path(localPath.getFileNameAsString());
        				manifest.add(new FileStagingObject(localPath, remotePath));
        			
        				fileInput.put("path", remotePath.toString());
	        		} else if (parameter.getType().equals("Directory")) {
	        			String paramId = null;
	        			if (jobOrder.containsKey(parameter.getId())) {
	        				paramId = parameter.getId();
	        			} else if (parameter.getId().startsWith("#")) {
	        				paramId = parameter.getId().split("/")[1];
	        			}
	        			
        				// This should either work or throw an exception, we know about the problem (type erasure).
        				@SuppressWarnings("unchecked")
        				HashMap<String, Object> fileInput = (HashMap<String, Object>) jobOrder.get(paramId);

        				Path localPath = new Path((String) fileInput.get("path"));
        				Path remotePath = new Path(localPath.getFileNameAsString());
        				manifest.add(new DirectoryStagingObject(localPath, remotePath));
        			
        				fileInput.put("path", remotePath.toString());
	        		}
	        	}
	        	
	        	String newJobOrderString = mapper.writeValueAsString(jobOrder);
	        	logger.debug("New job order string: " + newJobOrderString);
	        	manifest.add(new StringToFileStagingObject(newJobOrderString, remoteJobOrder));
	        }
	        
	        stager.stageIn(manifest);
	        
	        jobService.setXenonRemoteDir(jobId, service.getRemoteFileSystem().getWorkingDirectory().resolve(manifest.getTargetDirectory()));
	        jobService.setJobState(jobId, JobState.STAGING_IN, JobState.STAGING_READY);

	        jobLogger.info("StageIn complete.");
		} catch (XenonException | IOException | StatePreconditionException e) {
			jobLogger.error("Error during stage-in: ", e);
			logger.error("Error during stage-in: ", e);
			jobService.setErrorAndState(jobId, e, JobState.STAGING_IN, JobState.PERMANENT_FAILURE);
		}
	}
}

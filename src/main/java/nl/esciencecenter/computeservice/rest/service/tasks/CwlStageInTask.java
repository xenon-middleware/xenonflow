package nl.esciencecenter.computeservice.rest.service.tasks;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

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
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.computeservice.rest.service.staging.DirectoryStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.FileStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.StagingManifest;
import nl.esciencecenter.computeservice.rest.service.staging.StringToFileStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.XenonStager;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.Path;

public class CwlStageInTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(CwlStageInTask.class);
	
	private String jobId;
	private XenonService service;
	private JobRepository repository;

	public CwlStageInTask(String jobId, XenonService service) throws XenonException {
		this.jobId = jobId;
		this.service = service;
		this.repository = service.getRepository();
	}
	
	@Override
	public void run(){
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
		Job job = repository.findOne(jobId);
		try {
			XenonStager stager = new XenonStager(repository, service.getLocalFileSystem(), service.getRemoteFileSystem());

			// Staging files
			StagingManifest manifest = new StagingManifest(jobId, job.getSandboxDirectory());

	        // Add the workflow to the staging manifest
	        Path localWorkflow = new Path(job.getWorkflow());
	        Path workflowBaseName = new Path (localWorkflow.getFileNameAsString());
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
	        	Workflow workflow = Workflow.fromInputStream(service.getLocalFileSystem().readFromFile(localWorkflow));
	        	
	        	for (InputParameter parameter : workflow.getInputs()) {
	        		if (parameter.getType().equals("File")) {
	        			if (jobOrder.containsKey(parameter.getId())) {
	        				HashMap<String, Object> fileInput = (HashMap<String, Object>) jobOrder.get(parameter.getId());
	        				Path localPath = new Path((String) fileInput.get("path"));
	        				Path remotePath = new Path(localPath.getFileNameAsString());
	        				manifest.add(new FileStagingObject(localPath, remotePath));
	        			
	        				fileInput.put("path", remotePath.toString());
	        			}
	        		} else if (parameter.getType().equals("Directory")) {
	        			if (jobOrder.containsKey(parameter.getId())) {
	        				HashMap<String, Object> fileInput = (HashMap<String, Object>) jobOrder.get(parameter.getId());
	        				Path localPath = new Path((String) fileInput.get("path"));
	        				Path remotePath = new Path(localPath.getFileNameAsString());
	        				manifest.add(new DirectoryStagingObject(localPath, remotePath));
	        			
	        				fileInput.put("path", remotePath.toString());
	        			}
	        		}
	        	}
	        	
	        	String newJobOrderString = mapper.writeValueAsString(jobOrder);
	        	logger.debug("New job order string: " + newJobOrderString);
	        	manifest.add(new StringToFileStagingObject(newJobOrderString, remoteJobOrder));
	        }
	        
	        job = stager.stageIn(manifest, job);
	        
	        Path remoteDirectory = service.getRemoteFileSystem().getWorkingDirectory().resolve(manifest.getTargetDirectory());
	        job.getAdditionalInfo().put("xenon.remote.directory", remoteDirectory.toString());
	        
	        jobLogger.info("StageIn complete.\n\n");
	        job = repository.save(job);
		} catch (XenonException e) {
			jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
		} catch (JsonParseException e) {
			jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
		} catch (JsonMappingException e) {
			jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
		} catch (IOException e) {
			jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
		}
	}
}

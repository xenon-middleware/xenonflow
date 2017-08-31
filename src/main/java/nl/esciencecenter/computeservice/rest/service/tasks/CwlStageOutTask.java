package nl.esciencecenter.computeservice.rest.service.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.commonwl.cwl.OutputParameter;
import org.commonwl.cwl.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.model.JobState;
import nl.esciencecenter.computeservice.rest.model.StatePreconditionException;
import nl.esciencecenter.computeservice.rest.model.WorkflowBinding;
import nl.esciencecenter.computeservice.rest.service.JobService;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.computeservice.rest.service.staging.DirectoryStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.FileStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.StagingManifest;
import nl.esciencecenter.computeservice.rest.service.staging.XenonStager;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.Path;

public class CwlStageOutTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(CwlStageOutTask.class);
	
	private String jobId;
	private XenonService service;
	private JobRepository repository;
	private Integer exitcode;
	private JobService jobService;

	public CwlStageOutTask(String jobId, Integer exitcode, XenonService service) throws XenonException {
		this.jobId = jobId;
		this.service = service;
		this.repository = service.getRepository();
		this.jobService = service.getJobService();
		this.exitcode = exitcode;
	}
	
	@Override
	public void run() {
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
		Job job = repository.findOne(jobId);
		try {
			XenonStager stager = new XenonStager(jobService, repository, service.getSourceFileSystem(), service.getRemoteFileSystem());
	        // Staging back output
	        StagingManifest manifest = new StagingManifest(jobId, new Path("out/" + job.getId() + "/"));
	        
	        Path remoteDirectory = job.getSandboxDirectory();
	        Path outPath = remoteDirectory.resolve("stdout.txt");
	        Path errPath = remoteDirectory.resolve("stderr.txt");
	        Path localErrPath = new Path(errPath.getFileNameAsString());
	        
	        InputStream stderr = service.getRemoteFileSystem().readFromFile(errPath);
			String errorContents = IOUtils.toString(stderr, "UTF-8");
			if (errorContents.isEmpty()) {
	    		//throw new IOException("Output path " + outPath + " was empty!");
	    		jobLogger.warn("Error path " + errPath + " was empty!");
	    	} else {
	    		jobLogger.info("Standard Error:" + errorContents);
	    		manifest.add(new FileStagingObject(errPath, localErrPath));
	    	}
			
			InputStream stdout = service.getRemoteFileSystem().readFromFile(outPath);
	    	String outputContents = IOUtils.toString(stdout, "UTF-8");
	    	
	    	if (outputContents.isEmpty()) {
	    		//throw new IOException("Output path " + outPath + " was empty!");
	    		jobLogger.warn("Output path " + outPath + " was empty!");
	    	} else {
		    	jobLogger.info("Raw output: " + outputContents);
	  
		        WorkflowBinding outputMap = null;
		        // TODO: Try to stage back files even if the exitcode
		        // is not 0.
		        if (exitcode != null && exitcode.intValue() == 0) {
		        	// Add output from cwl run
		        	// Read in the workflow to get the outputs
		        	// TODO: Take care of optional outputs
		        	outputMap = addOutputStaging(manifest, new Path(job.getWorkflow()), outputContents);
		        }
		        
		        if (outputMap != null) {
		        	jobLogger.info("Fixed output: " + outputMap.toIndentedString());
		        	jobService.setOutputBinding(jobId, outputMap);
		        }
	    	}

	    	if (manifest.size() > 0) {
	    		WorkflowBinding files = stager.stageOut(manifest);
	    		jobService.setAdditionalInfo(jobId, "files", files);
	    	} else {
	    		jobLogger.warn("There are no files to stage.");
	    	}
	    	
	        jobLogger.info("StageOut complete.");
	        
	        if (!job.getInternalState().isFinal()) {
	        	if (exitcode == 0) {
	        		jobService.setJobState(jobId, JobState.STAGING_OUT, JobState.SUCCESS);
	        	} else {
	        		jobService.setJobState(jobId, JobState.STAGING_OUT, JobState.PERMANENT_FAILURE);
	        	}
	        }
		} catch (StatePreconditionException | IOException | XenonException e){
			jobLogger.error("Error during stage out of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during stage out of " + job.getName() + "(" +job.getId() +")", e);
		}
	}

	private WorkflowBinding addOutputStaging(StagingManifest manifest, Path localWorkflow, String outputContents) throws JsonParseException, JsonMappingException, IOException, XenonException {
		Path workflowPath = service.getSourceFileSystem().getWorkingDirectory().resolve(localWorkflow);
		Workflow workflow = Workflow.fromInputStream(service.getSourceFileSystem().readFromFile(workflowPath.toAbsolutePath()));
    	
    	// Read the cwltool stdout to determine where the files are.
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
    	WorkflowBinding outputMap = mapper.readValue(new StringReader(outputContents), WorkflowBinding.class);
    	
    	for (OutputParameter parameter : workflow.getOutputs()) {
    		if (parameter.getType().equals("File")) {
    			String paramId = null;
    			if (outputMap.containsKey(parameter.getId())) {
    				paramId = parameter.getId();
    			} else if (parameter.getId().startsWith("#")) {
    				paramId = parameter.getId().split("/")[1];
    			}
				// This should either work or throw an exception, we know about the problem (type erasure).
				@SuppressWarnings("unchecked")
				HashMap<String, Object> fileOutput = (HashMap<String, Object>) outputMap.get(paramId);

				Path remotePath = new Path((String) fileOutput.get("path")).toAbsolutePath();
				Path localPath = new Path(remotePath.getFileNameAsString());
				manifest.add(new FileStagingObject(remotePath, localPath));
			
				String fullPath = manifest.getTargetDirectory().resolve(localPath).toString();
				fileOutput.put("path", fullPath);
				
				UriComponentsBuilder b = UriComponentsBuilder.fromUriString(service.getSourceFileSystem().getLocation().toString());
				b.pathSegment(fullPath);
				fileOutput.put("location", b.build().toString());
    		} else if (parameter.getType().equals("Directory")) {
    			String paramId = null;
    			if (outputMap.containsKey(parameter.getId())) {
    				paramId = parameter.getId();
    			} else if (parameter.getId().startsWith("#")) {
    				paramId = parameter.getId().split("/")[1];
    			}
    			
    			// This should either work or throw an exception, we know about the problem (type erasure).
				@SuppressWarnings("unchecked")
    			HashMap<String, Object> dirOutput = (HashMap<String, Object>) outputMap.get(paramId);

				Path remotePath = new Path((String) dirOutput.get("path")).toAbsolutePath();
				Path localPath = new Path(remotePath.getFileNameAsString());
				manifest.add(new DirectoryStagingObject(remotePath, localPath));
			
				String fullPath = manifest.getTargetDirectory().resolve(localPath).toString();
				dirOutput.put("path", fullPath);
				
				UriComponentsBuilder b = UriComponentsBuilder.fromUriString(service.getSourceFileSystem().getLocation().toString());
				b.pathSegment(fullPath);
				dirOutput.put("location", b.build().toString());
    		}
    	}
    	
    	return outputMap;
	}
}

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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.computeservice.rest.service.staging.DirectoryStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.FileStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.FileToMapStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.StagingManifest;
import nl.esciencecenter.computeservice.rest.service.staging.XenonStager;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.schedulers.JobStatus;

public class CwlStageOutTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(CwlStageOutTask.class);
	
	private String jobId;
	private XenonService service;
	private JobRepository repository;
	private JobStatus status;

	public CwlStageOutTask(String jobId, JobStatus status, XenonService service) throws XenonException {
		this.jobId = jobId;
		this.service = service;
		this.repository = service.getRepository();
		this.status = status;
	}
	
	@Override
	public void run() {
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
		Job job = repository.findOne(jobId);
		try {
			XenonStager stager = new XenonStager(repository, service.getLocalFileSystem(), service.getRemoteFileSystem());
	        // Staging back output
	        StagingManifest manifest = new StagingManifest(jobId, new Path("out/" + job.getId() + "/"));
	        
	        Path remoteDirectory = job.getSandboxDirectory();
	        Path outPath = remoteDirectory.resolve("stdout.txt");
	        Path errPath = remoteDirectory.resolve("stderr.txt");
	        Path localErrPath = new Path(errPath.getFileNameAsString());
	        
	        manifest.add(new FileStagingObject(errPath, localErrPath));
	        
	        if (status.getExitCode() == 0) {
	        	// Add output from cwl run
	        	// Read in the workflow to get the required inputs
	        	job = addOutputStaging(manifest, job, outPath);
	        }
	        
	        job = stager.stageOut(manifest, job);
	        
	        jobLogger.info("StageOut complete.\n\n");
	        job = repository.save(job);
		} catch (XenonException e) {
			jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
		} catch (IOException e) {
			jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
		}
	}

	private Job addOutputStaging(StagingManifest manifest, Job job, Path outPath) throws JsonParseException, JsonMappingException, IOException, XenonException {
		Path localWorkflow = new Path(job.getWorkflow());
    	Workflow workflow = Workflow.fromInputStream(service.getLocalFileSystem().readFromFile(localWorkflow));
    	
    	// Read the cwltool stdout to determine where the files are.
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

    	InputStream stdout = service.getRemoteFileSystem().readFromFile(outPath);
    	String contents = IOUtils.toString(stdout, "UTF-8");
    	
    	if (contents.isEmpty()) {
    		throw new IOException("Output path " + outPath + " was empty!");
    	}

    	HashMap<String, Object> outputMap = mapper.readValue(new StringReader(contents), typeRef);
    	
    	for (OutputParameter parameter : workflow.getOutputs()) {
    		if (parameter.getType().equals("File")) {
    			if (outputMap.containsKey(parameter.getId())) {
    				HashMap<String, Object> fileOutput = (HashMap<String, Object>) outputMap.get(parameter.getId());
    				Path remotePath = new Path((String) fileOutput.get("path"));
    				Path localPath = new Path(remotePath.getFileNameAsString());
    				manifest.add(new FileStagingObject(remotePath, localPath));
    			
    				String fullPath = manifest.getTargetDirectory().resolve(localPath).toString();
    				fileOutput.put("path", fullPath);
    				
    				UriComponentsBuilder b = UriComponentsBuilder.fromUriString(service.getLocalFileSystem().getLocation().toString());
    				b.pathSegment(fullPath);
    				fileOutput.put("location", b.build().toString());
    			}
    		} else if (parameter.getType().equals("Directory")) {
    			HashMap<String, Object> dirOutput = (HashMap<String, Object>) outputMap.get(parameter.getId());
				Path remotePath = new Path((String) dirOutput.get("path"));
				Path localPath = new Path(remotePath.getFileNameAsString());
				manifest.add(new DirectoryStagingObject(remotePath, localPath));
			
				String fullPath = manifest.getTargetDirectory().resolve(localPath).toString();
				dirOutput.put("path", fullPath);
				
				UriComponentsBuilder b = UriComponentsBuilder.fromUriString(service.getLocalFileSystem().getLocation().toString());
				b.pathSegment(fullPath);
				dirOutput.put("location", b.build().toString());
    		}
    	}
    	
    	job.getOutput().put("stdout", outputMap);
    	return job;
	}

}

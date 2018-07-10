package nl.esciencecenter.computeservice.service.staging;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.commonwl.cwl.CwlException;
import org.commonwl.cwl.InputParameter;
import org.commonwl.cwl.Workflow;
import org.commonwl.cwl.utils.CWLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.StatePreconditionException;
import nl.esciencecenter.computeservice.model.XenonflowException;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class StagingManifestFactory {
	private static Logger logger = LoggerFactory.getLogger(StagingManifestFactory.class);

	public static StagingManifest createStagingManifest(Job job, FileSystem fileSystem, Logger jobLogger) throws CwlException, XenonException, JsonParseException, JsonMappingException, IOException, StatePreconditionException, XenonflowException {
		StagingManifest manifest = new StagingManifest(job.getId(), job.getSandboxDirectory());

        // Add the workflow to the staging manifest
        Path localWorkflow = new Path(job.getWorkflow());
        Path workflowBaseName = new Path (localWorkflow.getFileNameAsString());
        
		// Read in the workflow to get the required inputs
		Path workflowPath = fileSystem.getWorkingDirectory().resolve(localWorkflow);
		Path workflowBasePath = fileSystem.getWorkingDirectory().relativize(workflowPath.getParent());

		jobLogger.debug("Loading workflow from: " + workflowPath);
		String extension = FilenameUtils.getExtension(workflowPath.getFileNameAsString());
		Workflow workflow = Workflow.fromInputStream(fileSystem.readFromFile(workflowPath.toAbsolutePath()), extension);
		
		if (workflow == null || workflow.getSteps() == null) {
			throw new CwlException("Error staging files, cannot read the workflow file!\nworkflow: " + workflow);
		}
		
        manifest.add(new FileStagingObject(localWorkflow, workflowBaseName));
        addSubWorkflowsToManifest(workflow, manifest, workflowBasePath, fileSystem, jobLogger);
        
        addInputToManifest(job, workflow, manifest, jobLogger);
        
        return manifest;
	}
	
	public static void addSubWorkflowsToManifest(Workflow workflow, StagingManifest manifest, Path workflowBasePath, FileSystem fileSystem, Logger jobLogger) throws JsonParseException, JsonMappingException, IOException, XenonException, XenonflowException {
		// Recursively go through the workflow and get all the local cwl files
        List<Path> paths = CWLUtils.getLocalWorkflowPaths(workflow, workflowBasePath, fileSystem, jobLogger);
        for (Path path : paths) {
        	// TODO: The target path may be an absolute path or a weird location
        	// So we should probably update it and update the cwl file as well
        	Path localPath = path;
        	if (!localPath.isAbsolute()) {
        		localPath = workflowBasePath.resolve(path);
        	}
        	Path remotePath = path;
        	manifest.add(new FileStagingObject(localPath, remotePath));
        }
	}
	
	public static void addInputToManifest(Job job, Workflow workflow, StagingManifest manifest, Logger jobLogger) throws CwlException, JsonParseException, JsonMappingException, IOException, StatePreconditionException {
		Path remoteJobOrder = null;
		// TODO: Maybe also check if the workflow expects inputs
		// Actually, the logic is the wrong way around currently
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

			if (workflow.getInputs() == null) {
				throw new CwlException("Error staging files, cannot read the workflow file!\nworkflow: " + workflow);
			}
        	
			jobLogger.debug("Parsing inputs from: " + workflow.toString());
        	for (InputParameter parameter : workflow.getInputs()) {
    			String paramId = null;
    			if (parameter.getId().startsWith("#")) {
    				// If the parameter name start with # it likely
    				// has the format #main/param_name
    				paramId = parameter.getId().split("/")[1];
    			} else {
    				paramId = parameter.getId();
    			}
    			
    			// TODO: Support secondaryFiles
    			if (parameter.getType().equals("File?") || parameter.getType().equals("Directory?")) {
    				if (jobOrder.containsKey(paramId)) {
    					addFileOrDirectoryToManifest(manifest, parameter, jobOrder, paramId);
    				}
    			} else if (parameter.getType().equals("File") || parameter.getType().equals("Directory")) {
    				if (!jobOrder.containsKey(paramId)) {
        				throw new CwlException("Error staging files, cannot find: " + paramId + " in the job order.");
        			} else {
        				addFileOrDirectoryToManifest(manifest, parameter, jobOrder, paramId);
        			}
    			} else if (parameter.getType().equals("File[]") || parameter.getType().equals("Directory[]")) {
    				// TODO: Implement array inputs
    				throw new NotImplementedException("File and Directory arrays are not yet implement (sorry!)");
    			}
        	}
        
    		String newJobOrderString = mapper.writeValueAsString(jobOrder);
    		logger.debug("New job order string: " + newJobOrderString);
    		manifest.add(new StringToFileStagingObject(newJobOrderString, remoteJobOrder));
        }
	}
	
	private static void addFileOrDirectoryToManifest(StagingManifest manifest, InputParameter parameter, HashMap<String, Object> jobOrder, String paramId) {
		// This should either work or throw an exception. We can't make this a checked cast
		// so suppress the warning
		@SuppressWarnings("unchecked")
		HashMap<String, Object> fileInput = (HashMap<String, Object>) jobOrder.get(paramId);
		
		Path localPath = new Path((String) fileInput.get("path"));
		Path remotePath = new Path(localPath.getFileNameAsString());
		fileInput.put("path", remotePath.toString());

		if (parameter.getType().equals("File") || parameter.getType().equals("File?")) {
			manifest.add(new FileStagingObject(localPath, remotePath));
		} else if (parameter.getType().equals("Directory") || parameter.getType().equals("Directory?")) {
			manifest.add(new DirectoryStagingObject(localPath, remotePath));
		}
	}
}

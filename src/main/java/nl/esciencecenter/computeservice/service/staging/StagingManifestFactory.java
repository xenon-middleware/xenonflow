package nl.esciencecenter.computeservice.service.staging;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.commonwl.cwl.CwlException;
import org.commonwl.cwl.FileDirEntry;
import org.commonwl.cwl.InputParameter;
import org.commonwl.cwl.OutputParameter;
import org.commonwl.cwl.Parameter;
import org.commonwl.cwl.Workflow;
import org.commonwl.cwl.utils.CWLUtils;
import org.commonwl.cwl.utils.CWLUtils.WorkflowDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.StatePreconditionException;
import nl.esciencecenter.computeservice.model.WorkflowBinding;
import nl.esciencecenter.computeservice.model.XenonflowException;
import nl.esciencecenter.computeservice.service.JobService;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class StagingManifestFactory {
	private static Logger logger = LoggerFactory.getLogger(StagingManifestFactory.class);
	
	public static StagingManifest createStagingInManifest(Job job, FileSystem cwlFileSystem, FileSystem sourceFileSystem, String cwlCommandScript, Logger jobLogger, JobService jobService) throws CwlException, XenonException, JsonParseException, JsonMappingException, IOException, StatePreconditionException, XenonflowException {
		StagingManifest manifest = new StagingManifest(job.getId(), job.getSandboxDirectory());

		WorkflowDescription wfd = CWLUtils.loadLocalWorkflow(job, cwlFileSystem, jobLogger);

		if (wfd.workflow == null || wfd.workflow.getSteps() == null) {
			throw new CwlException("Error staging files, cannot read the workflow file!\nworkflow: " + wfd.workflow);
		}
		
		if (cwlCommandScript == null) {
			manifest.add(new CommandScriptStagingObject("#!/usr/bin/env bash\n\ncwltool $@", new Path("cwlcommand"), null));
		} else {
			manifest.add(new CommandScriptStagingObject(cwlCommandScript, new Path("cwlcommand"), null));
		}
		
        manifest.add(new CwlFileStagingObject(wfd.localPath, wfd.workflowBaseName, null));
        addSubWorkflowsToManifest(wfd.workflow, manifest, wfd.workflowBasePath, cwlFileSystem, jobLogger);

        addInputToManifest(job, wfd.workflow, manifest, jobLogger, jobService);

        return manifest;
	}
	
	public static StagingManifest createStagingOutManifest(Job job, Integer exitcode, FileSystem cwlFileSystem, FileSystem fileSystem, FileSystem remoteFileSystem, JobService jobService,
			Logger jobLogger) throws JsonParseException, JsonMappingException, IOException, XenonException, XenonflowException, CwlException {
		StagingManifest manifest = new StagingManifest(job.getId(), job.getSandboxDirectory());
		manifest.setBaseurl((String) job.getAdditionalInfo().get("baseurl"));
		
		WorkflowDescription wfd = CWLUtils.loadLocalWorkflow(job, cwlFileSystem, jobLogger);
		
		Path remoteDirectory = job.getSandboxDirectory();
        Path outPath = remoteDirectory.resolve("stdout.txt");
        Path errPath = remoteDirectory.resolve("stderr.txt");
        Path localErrPath = new Path(errPath.getFileNameAsString());
        
        InputStream stderr = remoteFileSystem.readFromFile(errPath);
		String errorContents = IOUtils.toString(stderr, "UTF-8");
		if (errorContents.isEmpty()) {
    		//throw new IOException("Output path " + outPath + " was empty!");
    		jobLogger.warn("Error path " + errPath + " was empty!");
    	} else {
    		jobLogger.info("Standard Error:" + errorContents);
    		manifest.add(new FileStagingObject(errPath, localErrPath, null));
    	}
		
		InputStream stdout = remoteFileSystem.readFromFile(outPath);
    	String outputContents = IOUtils.toString(stdout, "UTF-8");
    	
    	if (outputContents.isEmpty()) {
    		//throw new IOException("Output path " + outPath + " was empty!");
    		jobLogger.warn("Output path " + outPath + " was empty!");
    	} else {
	    	jobLogger.info("Raw output: " + outputContents);
  
	        WorkflowBinding outputMap = null;
	        // TODO: Try to stage back files even if the exitcode is not 0.
	        if (exitcode != null && exitcode.intValue() == 0) {
	        	outputMap = addOutputToManifest(job, wfd.workflow, manifest, outputContents, jobLogger);
	        }
	        
	        if (outputMap != null) {
	        	jobService.setOutputBinding(job.getId(), outputMap);
	        }
    	}
		
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
        	manifest.add(new CwlFileStagingObject(localPath, remotePath, null));
        }
	}
	
	public static void addInputToManifest(Job job, Workflow workflow, StagingManifest manifest, Logger jobLogger, JobService jobService) throws CwlException, JsonParseException, JsonMappingException, IOException, StatePreconditionException, XenonflowException, XenonException {	
		// Read in the job order as a hashmap
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		TypeReference<WorkflowBinding> typeRef = new TypeReference<WorkflowBinding>() {};
	
		if (workflow.getInputs() == null) {
			throw new CwlException("Error staging files, cannot read the workflow file!\nworkflow: " + workflow);
		}

		// Add files and directories from the input to the staging
    	// manifest and update the input to point to locations
    	// on the remote server
		WorkflowBinding jobOrder = null;
		if(job.hasInput()) {
			String jobOrderString = job.getInput().toString();
			logger.debug("Old job order string: " + jobOrderString);
			jobOrder = mapper.readValue(new StringReader(jobOrderString), typeRef);
		} else {
			jobOrder = new WorkflowBinding();
		}
        if (workflow.getInputs().length > 0) {
        	Path remoteJobOrder = new Path("job-order.json");

			jobLogger.debug("Parsing inputs from: " + workflow.toString());
        	for (InputParameter parameter : workflow.getInputs()) {
        		if (parameter.getId().equals("xenonflow_jobid")) {
        			jobOrder.put("xenonflow_jobid", job.getId());
        		} else if (parameter.getId().equals("xenonflow_jobname")) {
        			jobOrder.put("xenonflow_jobname", job.getName());
        		} else {
        			addParameterToManifest(manifest, jobOrder, parameter);
        		}
        	}
    	
			String newJobOrderString = mapper.writeValueAsString(jobOrder);
			logger.debug("New job order string: " + newJobOrderString);
			manifest.add(new StringToFileStagingObject(newJobOrderString, remoteJobOrder, null));
			
			jobService.setInput(job.getId(), jobOrder);
        }
	}
	
	private static WorkflowBinding addOutputToManifest(Job job, Workflow workflow, StagingManifest manifest, String outputContents, Logger jobLogger) throws JsonParseException, JsonMappingException, IOException, XenonException, XenonflowException, CwlException {    	
    	// Read the cwltool stdout to determine where the files are.
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
    	WorkflowBinding outputMap = mapper.readValue(new StringReader(outputContents), WorkflowBinding.class);
    	
    	if (workflow.getOutputs().length > 0) {
	    	for (OutputParameter parameter : workflow.getOutputs()) {
	    		addParameterToManifest(manifest, outputMap, parameter);
	    	}
    	}
    	
    	return outputMap;
	}

	private static void addParameterToManifest(StagingManifest manifest, HashMap<String, Object> map, Parameter parameter) throws CwlException, MalformedURLException, XenonflowException, XenonException {
		String paramId = null;
		if (parameter.getId().startsWith("#") && parameter.getId().contains("/")) {
			// If the parameter name start with # it likely
			// has the format #main/param_name
			paramId = parameter.getId().split("/")[1];
		} else {
			paramId = parameter.getId();
		}

		// TODO: Support secondaryFiles
		if (parameter.isOptional()) {
			if (map.containsKey(paramId)) {
				addFileOrDirectoryToManifest(manifest, parameter, map, paramId);
			}
		} else if (parameter.getType().equals("File")
				 || parameter.getType().equals("File?")
				 || parameter.getType().equals("Directory")
				 || parameter.getType().equals("Directory?")
				 || parameter.getType().equals("stdout")
				 || parameter.getType().equals("stderr")) {
			if (!map.containsKey(paramId) && !parameter.containsKey("default")) {
				throw new CwlException("Error staging files, cannot find: " + paramId + " in the job order.");
			} else {
				addFileOrDirectoryToManifest(manifest, parameter, map, paramId);
			}
		} else if (parameter.getType().equals("File[]") || parameter.getType().equals("Directory[]")) {
			addFileOrDirectoryArrayToManifest(manifest, parameter, map, paramId);
		}
	}

	private static void addFileOrDirectoryArrayToManifest(StagingManifest manifest, Parameter parameter, HashMap<String, Object> jobOrder, String paramId) {
		@SuppressWarnings("unchecked")
		List<HashMap<String, Object>> arrayInput = (List<HashMap<String, Object> >) jobOrder.get(paramId);
		for (HashMap<String, Object> entry : arrayInput) {
			FileDirEntry fileDirEntry = FileDirEntry.fromHashMap(entry);
			Path localPath = null;
			if (fileDirEntry.hasPath()) {
				localPath = new Path((String) fileDirEntry.getPath());
			} else if (fileDirEntry.hasLocation() && fileDirEntry.isLocalLocation()) {
				localPath = new Path((String) fileDirEntry.getLocation());
			}
			
			Path remotePath = new Path(localPath.getFileNameAsString());
			entry.put("path", remotePath.toString());
			
			if (parameter.getType().equals("File[]")) {
				manifest.add(new FileStagingObject(localPath, remotePath, parameter));
			} else if (parameter.getType().equals("Directory[]")) {
				manifest.add(new DirectoryStagingObject(localPath, remotePath, parameter));
			}
		}
	}
	
	private static void addFileOrDirectoryToManifest(StagingManifest manifest, Parameter parameter, HashMap<String, Object> map, String paramId) throws MalformedURLException, XenonflowException, XenonException {
		if (parameter.getType().equals("File") || parameter.getType().equals("File?")
				|| parameter.getType().equals("stdout") || parameter.getType().equals("stderr")) {
			addFileToManifest(manifest, parameter, map, paramId);
		} else if (parameter.getType().equals("Directory") || parameter.getType().equals("Directory?")) {
			addDirectoryToManifest(manifest, parameter, map, paramId);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void addFileToManifest(StagingManifest manifest, Parameter parameter, HashMap<String, Object> map, String paramId) throws MalformedURLException, XenonflowException, XenonException {		
		HashMap<String, Object> file = null;
		if (map.containsKey(paramId)) {
			// This should either work or throw an exception. We can't make this a checked cast
			// so suppress the warning
			file = (HashMap<String, Object>) map.get(paramId);
			if (file == null) {
				file = (HashMap<String, Object>) parameter.get("default");
			}
		} else {
			// There should be a default now if it was not in the map
			file = (HashMap<String, Object>) parameter.get("default");
		}
		
		if (file == null) {
			// We did not find any suitable value for this file.
			if (parameter.isOptional()) {
				// But it's an optional file, so skip it.
				return;
			} else {
				// We bail out here as we do not support this particular case
				throw new XenonflowException("Could not find a file to stage for parameter: " + paramId);
			}
		}
		
		Path sourcePath = null;
		Path targetPath = null;
		if (!file.containsKey("path") && !file.containsKey("location") && file.containsKey("contents")) {
			
			if (file.containsKey("basename")) {
				targetPath = new Path((String)file.get("basename"));
			} else { 
				targetPath = new Path(paramId);
			}
			
			manifest.add(new StringToFileStagingObject((String) file.get("contents"), targetPath, parameter));
		} else {
			if (file.containsKey("path")) {
				sourcePath = new Path((String) file.get("path"));
			} else if (file.containsKey("location")) {
				String location = (String)file.get("location");
				if (CWLUtils.isPathNotLocation(location)) {
					// it is a path but set in location
					sourcePath = new Path(location);
				} else if (CWLUtils.isLocalPath(location)) {
					// it is an actual local path with file:// in front
					sourcePath = CWLUtils.getLocalPath(location);
				}
			} else if(file.containsKey("basename")) {
				sourcePath = CWLUtils.getLocalPath((String)file.get("basename"));
			}
			targetPath = new Path(sourcePath.getFileNameAsString());
			manifest.add(new FileStagingObject(sourcePath, targetPath, parameter));
		}
		
		file.put("path", targetPath.toString());
	}
	
	private static void addDirectoryToManifest(StagingManifest manifest, Parameter parameter, HashMap<String, Object> map, String paramId) throws MalformedURLException, XenonflowException, XenonException {
		// This should either work or throw an exception. We can't make this a checked cast
		// so suppress the warning
		@SuppressWarnings("unchecked")
		HashMap<String, Object> dir = (HashMap<String, Object>) map.get(paramId);
		
		Path sourcePath = null;
		if (dir.containsKey("path")) {
			sourcePath = new Path((String) dir.get("path"));
		} else if (dir.containsKey("location")) {
			String location = (String)dir.get("location");
			if (CWLUtils.isPathNotLocation(location)) {
				// it is a path but set in location
				sourcePath = new Path(location);
			} else if (CWLUtils.isLocalPath(location)) {
				// it is an actual local path with file:// in front
				sourcePath = CWLUtils.getLocalPath(location);
			}
		} else if(dir.containsKey("basename")) {
			sourcePath = CWLUtils.getLocalPath((String)dir.get("basename"));
		}

		Path targetPath = new Path(sourcePath.getFileNameAsString());
		manifest.add(new DirectoryStagingObject(sourcePath, targetPath, parameter));

		dir.put("path", targetPath.toString());
	}
}
 
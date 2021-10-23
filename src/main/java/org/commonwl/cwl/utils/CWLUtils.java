package org.commonwl.cwl.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.commonwl.cwl.RunCommand;
import org.commonwl.cwl.Step;
import org.commonwl.cwl.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.XenonflowException;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class CWLUtils {
	public static class WorkflowDescription {
		public Workflow workflow;
		public Path localPath;
		public Path workflowBaseName;
		public Path workflowBasePath;
	}
	
	private static Logger logger = LoggerFactory.getLogger(CWLUtils.class);

	/**
	 * Recursively finds all paths refering to local workflow paths
	 * @param Workflow
	 * @return List of Xenon Paths
	 * @throws XenonException 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 * @throws XenonflowException 
	 */
	public static List<Path> getLocalWorkflowPaths(Workflow workflow, Path workflowBasePath, FileSystem fileSystem, Logger jobLogger) throws JsonParseException, JsonMappingException, IOException, XenonException, XenonflowException {
		List<Path> paths = new LinkedList<Path>();
		for (Step step : workflow.getSteps()) {
			RunCommand run = step.getRun();
			if (!run.isSubWorkflow()) {
				String path = run.getWorkflowPath();
				if (!CWLUtils.isWorkflowReference(path)&& CWLUtils.isLocalPath(path)) {
					paths.add(new Path(path));
					
					Path subWorkflowPath = workflowBasePath.resolve(path);
					Path subWorkflowBasePath = subWorkflowPath.getParent();
					logger.debug("Loading sub-workflow from: " + subWorkflowPath);
					String extension = FilenameUtils.getExtension(subWorkflowPath.getFileNameAsString());
					
					Path absoluteWorkflowPath = fileSystem.getWorkingDirectory().resolve(subWorkflowPath).normalize().toAbsolutePath();
					if (!absoluteWorkflowPath.startsWith(fileSystem.getWorkingDirectory())) {
						String error = String.format("A subworkflow with path: %s is in a directory that is not a child of the input directory: %s",
									  absoluteWorkflowPath, fileSystem.getWorkingDirectory());
						jobLogger.error(error);
						throw new XenonflowException(error);
					} else {
						Workflow subworkflow = Workflow.fromInputStream(fileSystem.readFromFile(absoluteWorkflowPath), extension);
						paths.addAll(CWLUtils.getLocalWorkflowPaths(subworkflow, subWorkflowBasePath, fileSystem, jobLogger));
					}
				}
			} else if (run.getSubWorkflow().isWorkflow()) {
				Path subWorkflowPath = workflowBasePath.resolve(run.getWorkflowPath());
				Path subWorkflowBasePath = subWorkflowPath.getParent();
				paths.addAll(CWLUtils.getLocalWorkflowPaths((Workflow)run.getSubWorkflow(), subWorkflowBasePath, fileSystem, jobLogger));
			}
		}

		return paths;
	}
	
	/**
	 * Loads a local file as a WorkflowDiscreption
	 * 
	 * @param job
	 * @param cwlFileSystem
	 * @param jobLogger
	 * @return WorkflowDescription
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws XenonException
	 */
	public static WorkflowDescription loadLocalWorkflow(Job job, FileSystem cwlFileSystem, Logger jobLogger) throws JsonParseException, JsonMappingException, IOException, XenonException {
		WorkflowDescription output = new WorkflowDescription();
		
		String workflow = job.getWorkflow();
		if (workflow.contains("#")) {
			workflow = workflow.split("#")[0];
		}
			
		
        // Add the workflow to the staging manifest
        Path localWorkflow = new Path(workflow);
        output.localPath = localWorkflow;
        output.workflowBaseName = new Path (localWorkflow.getFileNameAsString());

		// Read in the workflow to get the required inputs
		Path workflowPath = cwlFileSystem.getWorkingDirectory().resolve(localWorkflow);
		output.workflowBasePath = cwlFileSystem.getWorkingDirectory().relativize(workflowPath.getParent());

		jobLogger.debug("Loading workflow from: " + workflowPath);
		String extension = FilenameUtils.getExtension(workflowPath.getFileNameAsString());
		output.workflow = Workflow.fromInputStream(cwlFileSystem.readFromFile(workflowPath.toAbsolutePath()), extension);

		return output;
	}
	
	public static boolean isLocalWorkflow(Path workflow, FileSystem cwlFileSystem) throws XenonException {
		Path workflowPath = cwlFileSystem.getWorkingDirectory().resolve(workflow);
		
		return cwlFileSystem.exists(workflowPath);
	}
	
	/**
	 * Recursively finds all paths that the workflow refers to
	 * @param Workflow
	 * @return List of string paths
	 */
	public static List<String> getWorkflowPaths(Workflow workflow) {
		List<String> paths = new LinkedList<String>();

		for (Step step : workflow.getSteps()) {
			RunCommand run = step.getRun();
			if (!run.isSubWorkflow()) {
				paths.add(run.getWorkflowPath());
			} else if (run.getSubWorkflow().isWorkflow()) {
				paths.addAll(CWLUtils.getWorkflowPaths((Workflow) run.getSubWorkflow()));
			}
		}

		return paths;
	}
	
	public static boolean isPathNotLocation(String path) {
		try {
			// If it is an url this will not throw an exception
			// and thus is a location
			new URL(path);
			return false;
		} catch (MalformedURLException e) {
			// It's not a URL, so we should interpret it as a path
			return true;
		}
	}

	/**
	 * Determines if
	 * 
	 * @param path
	 *            - String
	 * @return true if path is a relative or absolute path or a url with a
	 *         file:// protocol, false otherwise
	 */
	public static boolean isLocalPath(String path) {
		try {
			URL url = new URL(path);
			return url.getProtocol().equals("file");
		} catch (MalformedURLException e) {
			// It's not a URL, so let's assume it's a path
			// and fail down the line if it's not correct
			return true;
		}
	}
	
	public static boolean isWorkflowReference(String path) {
		return path.startsWith("#");
	}
	
	/**
	 * Gets the local path from a file:// uri
	 * 
	 * @param uri
	 *            - String
	 * @return the path of the uri
	 * @throws MalformedURLException 
	 * @throws XenonflowException 
	 */
	public static Path getLocalPath(String uri) throws MalformedURLException, XenonflowException {
		URL url = new URL(uri);
		if (url.getProtocol().equals("file")) {
			return new Path(url.getPath());
		} else {
			throw new XenonflowException(String.format("%s is not a file url:", uri));
		}
	}
}

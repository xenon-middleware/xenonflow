package org.commonwl.cwl.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.commonwl.cwl.Step;
import org.commonwl.cwl.Workflow;
import org.commonwl.cwl.WorkflowStep;

import nl.esciencecenter.xenon.filesystems.Path;

public class CWLUtils {
	/**
	 * Recursively finds all paths refering to la
	 * @param Workflow
	 * @return List of Xenon Paths
	 */
	public static List<Path> getLocalWorkflowPaths(Workflow workflow) {
		List<Path> paths = new LinkedList<Path>();

		for (Step step : workflow.getSteps()) {
			WorkflowStep run = step.getRun();
			if (!run.isSubWorkflow()) {
				if (CWLUtils.isLocalPath(run.getWorkflowPath())) {
					paths.add(new Path(run.getWorkflowPath()));
				}
			} else if (run.getSubWorkflow().isWorkflow()) {
				paths.addAll(CWLUtils.getLocalWorkflowPaths((Workflow) run.getSubWorkflow()));
			}
		}

		return paths;
	}
	
	/**
	 * Recursively finds all paths that the workflow refers to
	 * @param Workflow
	 * @return List of string paths
	 */
	public static List<String> getWorkflowPaths(Workflow workflow) {
		List<String> paths = new LinkedList<String>();

		for (Step step : workflow.getSteps()) {
			WorkflowStep run = step.getRun();
			if (!run.isSubWorkflow()) {
				paths.add(run.getWorkflowPath());
			} else if (run.getSubWorkflow().isWorkflow()) {
				paths.addAll(CWLUtils.getWorkflowPaths((Workflow) run.getSubWorkflow()));
			}
		}

		return paths;
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
			if (url.getProtocol().equals("file")) {
				return true;
			}
			return false;
		} catch (MalformedURLException e) {
			// It's not a URL, so let's assume it's a path
			// and fail down the line if it's not correct
			return true;
		}
	}
}

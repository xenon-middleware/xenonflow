package org.commonwl.cwl;

/**
 * This class represents the duality of the RunCommand of a step
 * it can either be a string representing a path or url to a workflow file
 * or an in-line representation of a workflow (both Workflow and CommandLineTool)
 * 
 * @author bweel
 *
 */
public class RunCommand {
	private String workflowPath;
	private Process subWorkflow;
	
	public String getWorkflowPath() {
		return workflowPath;
	}
	
	public void setWorkflowPath(String workflowPath) {
		this.workflowPath = workflowPath;
	}
	
	public boolean isSubWorkflow() {
		return subWorkflow != null;
	}
	
	public Process getSubWorkflow() {
		return subWorkflow;
	}
	
	public void setSubWorkflow(Process subWorkflow) {
		this.subWorkflow = subWorkflow;
	}
}
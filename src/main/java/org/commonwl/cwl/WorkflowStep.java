package org.commonwl.cwl;

public class WorkflowStep {
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
package nl.esciencecenter.computeservice.service.staging;

import org.commonwl.cwl.Parameter;

public interface StagingObject {

	public void setBytesCopied(long bytes);
	public long getBytesCopied();
	
	public String toString();

	public void setCopyId(String copyId);
	public String getCopyId();
	
	public void setParameter(Parameter parameter);
	public Parameter getParameter();
}
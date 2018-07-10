package nl.esciencecenter.computeservice.service.staging;

public interface StagingObject {

	public void setBytesCopied(long bytes);
	public long getBytesCopied();
	
	public String toString();
	public void setCopyId(String copyId);
	public String getCopyId();
}
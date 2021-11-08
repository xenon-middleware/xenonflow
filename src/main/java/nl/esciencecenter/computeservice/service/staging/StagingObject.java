package nl.esciencecenter.computeservice.service.staging;

import org.commonwl.cwl.Parameter;
import org.slf4j.Logger;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public interface StagingObject {

	public void setBytesCopied(long bytes);
	public long getBytesCopied();
	
	public String toString();

	public void setCopyId(String copyId);
	public String getCopyId();
	
	public void setParameter(Parameter parameter);
	public Parameter getParameter();
	
	public String stage(Logger jobLogger, FileSystem sourceFileSystem, FileSystem targetFileSystem,
			Path sourceDirectory, Path targetDirectory) throws XenonException;
}
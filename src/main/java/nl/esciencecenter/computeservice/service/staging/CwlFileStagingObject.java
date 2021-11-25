package nl.esciencecenter.computeservice.service.staging;

import org.slf4j.Logger;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class CwlFileStagingObject extends FileStagingObject {
	protected FileSystem cwlFileSystem;

	public CwlFileStagingObject(Path sourcePath, Path targetPath, FileSystem cwlFileSystem) {
		super(sourcePath, targetPath, null);
		this.cwlFileSystem = cwlFileSystem;
	}

	@Override
	public String toString() {
		return "CwlFileStagingObject [sourcePath=" + sourcePath + ", targetPath=" + targetPath
				+ ", parameter=" + parameter + "]";
	}
	
	public String stage(Logger jobLogger, FileSystem sourceFileSystem, FileSystem targetFileSystem,
			Path sourceDirectory, Path targetDirectory) throws XenonException {		
		return super.stage(jobLogger, cwlFileSystem, targetFileSystem, sourceDirectory, targetDirectory);
	}
}
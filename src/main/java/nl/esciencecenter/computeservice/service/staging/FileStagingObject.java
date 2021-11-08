package nl.esciencecenter.computeservice.service.staging;

import org.commonwl.cwl.Parameter;
import org.slf4j.Logger;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.CopyMode;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class FileStagingObject extends BaseStagingObject implements FileOrDirectoryStagingObject {
	protected Path sourcePath;
	protected Path targetPath;

	public FileStagingObject(Path sourcePath, Path targetPath, Parameter parameter) {
		super(parameter);
		this.sourcePath = sourcePath;
		this.targetPath = targetPath;
	}

	public Path getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(Path sourcePath) {
		this.sourcePath = sourcePath;
	}

	public Path getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(Path targetPath) {
		this.targetPath = targetPath;
	}

	@Override
	public String toString() {
		return "FileStagingObject [sourcePath=" + sourcePath + ", targetPath=" + targetPath
				+ ", parameter=" + parameter + "]";
	}
	
	public String stage(Logger jobLogger, FileSystem sourceFileSystem, FileSystem targetFileSystem,
			Path sourceDirectory, Path targetDirectory) throws XenonException {		
		Path sourcePath = getSourcePath();
		if (!sourcePath.isAbsolute()) {
			sourcePath = sourceDirectory.resolve(getSourcePath()).toAbsolutePath();
		}
		Path targetPath = targetDirectory.resolve(getTargetPath());
		Path targetDir = targetPath.getParent();
		if (!targetFileSystem.exists(targetDir)) {
			targetFileSystem.createDirectories(targetDir);
		}
		
		jobLogger.info("Copying from " + sourcePath + " to " + targetPath);
		String copyId = sourceFileSystem.copy(sourcePath, targetFileSystem, targetPath, CopyMode.REPLACE,
				false);
		
		setCopyId(copyId);
		return copyId;
	}
}
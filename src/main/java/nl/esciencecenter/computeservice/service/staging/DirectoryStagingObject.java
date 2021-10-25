package nl.esciencecenter.computeservice.service.staging;

import org.commonwl.cwl.Parameter;

import nl.esciencecenter.xenon.filesystems.Path;

public class DirectoryStagingObject extends BaseStagingObject implements FileOrDirectoryStagingObject {
	private Path sourcePath;
	private Path targetPath;

	public DirectoryStagingObject(Path sourcePath, Path targetPath, Parameter parameter) {
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
		return "DirectoryStagingObject [sourcePath=" + sourcePath + ", targetPath=" + targetPath
				+ ", parameter=" + parameter + "]";
	}
}

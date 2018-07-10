package nl.esciencecenter.computeservice.service.staging;

import nl.esciencecenter.xenon.filesystems.Path;

public class FileStagingObject extends BaseStagingObject {
	private Path sourcePath;
	private Path targetPath;

	public FileStagingObject(Path sourcePath, Path targetPath) {
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
		return "FileStagingObject [sourcePath=" + sourcePath + ", targetPath=" + targetPath + "]";
	}
}
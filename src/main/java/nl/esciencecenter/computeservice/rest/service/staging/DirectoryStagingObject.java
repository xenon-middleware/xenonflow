package nl.esciencecenter.computeservice.rest.service.staging;

import nl.esciencecenter.xenon.filesystems.Path;

public class DirectoryStagingObject extends BaseStagingObject {
	private Path sourcePath;
	private Path targetPath;

	public DirectoryStagingObject(Path sourcePath, Path targetPath) {
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
}

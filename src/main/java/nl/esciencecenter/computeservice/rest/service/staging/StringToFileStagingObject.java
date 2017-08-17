package nl.esciencecenter.computeservice.rest.service.staging;

import nl.esciencecenter.xenon.filesystems.Path;

public class StringToFileStagingObject implements StagingObject {
	private String sourceString;
	private Path targetPath;

	public StringToFileStagingObject(String source, Path targetPath) {
		this.sourceString = source;
		this.targetPath = targetPath;
	}

	public String getSourceString() {
		return sourceString;
	}

	public void setSourceString(String sourceString) {
		this.sourceString = sourceString;
	}

	public Path getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(Path targetPath) {
		this.targetPath = targetPath;
	}
}
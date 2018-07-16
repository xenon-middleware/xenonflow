package nl.esciencecenter.computeservice.service.staging;

import nl.esciencecenter.xenon.filesystems.Path;

public class StringToFileStagingObject extends BaseStagingObject {
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

	@Override
	public String toString() {
		return "StringToFileStagingObject [sourceString=" + sourceString + ", targetPath=" + targetPath + "]";
	}
}
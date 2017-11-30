package nl.esciencecenter.computeservice.rest.service.staging;

import nl.esciencecenter.xenon.filesystems.Path;

public class FileToStringStagingObject extends BaseStagingObject {
	private Path sourcePath;
	private String targetString;

	public FileToStringStagingObject(Path sourcePath, String targetString) {
		this.sourcePath = sourcePath;
		this.targetString = targetString;
	}

	public Path getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(Path sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getTargetString() {
		return targetString;
	}

	public void setTargetString(String targetString) {
		this.targetString = targetString;
	}
}
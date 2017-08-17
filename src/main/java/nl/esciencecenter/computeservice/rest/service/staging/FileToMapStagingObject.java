package nl.esciencecenter.computeservice.rest.service.staging;

import nl.esciencecenter.xenon.filesystems.Path;

public class FileToMapStagingObject implements StagingObject {
	private Path sourcePath;
	private String targetString;

	public FileToMapStagingObject(Path sourcePath, String targetString) {
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

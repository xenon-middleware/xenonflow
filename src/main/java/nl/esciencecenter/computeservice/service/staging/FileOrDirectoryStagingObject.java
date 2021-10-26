package nl.esciencecenter.computeservice.service.staging;

import nl.esciencecenter.xenon.filesystems.Path;

public interface FileOrDirectoryStagingObject {
	public Path getTargetPath();

	public void setTargetPath(Path targetPath);
}

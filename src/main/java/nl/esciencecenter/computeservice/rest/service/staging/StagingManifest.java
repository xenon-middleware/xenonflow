package nl.esciencecenter.computeservice.rest.service.staging;

import java.util.LinkedList;

import nl.esciencecenter.xenon.filesystems.Path;

public class StagingManifest extends LinkedList<StagingObject> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3097185684821951169L;
	private String jobId;
	private Path targetDir;
	private String baseurl;
	
	public StagingManifest(String jobId, Path targetDir) {
		super();
		this.jobId = jobId;
		this.targetDir = targetDir;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public void setTargetDirectory(Path targetDir) {
		this.targetDir = targetDir;
	}
	
	public Path getTargetDirectory() {
		return targetDir;
	}

	public String getBaseurl() {
		return baseurl;
	}

	public void setBaseurl(String baseurl) {
		this.baseurl = baseurl;
	}

	public StagingObject getByCopyid(String id) {
		return this.stream().filter(o -> o.getCopyId() == id).findFirst().get();
	}
}

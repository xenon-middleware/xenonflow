package nl.esciencecenter.xenon.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public class ComputeResource {
	private AdaptorConfig scheduler;
	private AdaptorConfig filesystem;
	
	public AdaptorConfig getScheduler() {
		return scheduler;
	}

	public void setScheduler(AdaptorConfig scheduler) {
		this.scheduler = scheduler;
	}

	public AdaptorConfig getFilesystem() {
		return filesystem;
	}

	public void setFilesystem(AdaptorConfig filesystem) {
		this.filesystem = filesystem;
	}
}

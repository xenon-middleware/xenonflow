package nl.esciencecenter.xenon.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = false)
public class ComputeResource {
	@JsonProperty("scheduler")
	private AdaptorConfig schedulerConfig;
	@JsonProperty("filesystem")
	private AdaptorConfig filesystemConfig;
	
	public AdaptorConfig getSchedulerConfig() {
		return schedulerConfig;
	}

	public void setSchedulerConfig(AdaptorConfig schedulerConfig) {
		this.schedulerConfig = schedulerConfig;
	}

	public AdaptorConfig getFilesystemConfig() {
		return filesystemConfig;
	}

	public void setFilesystemConfig(AdaptorConfig filesystemConfig) {
		this.filesystemConfig = filesystemConfig;
	}
}

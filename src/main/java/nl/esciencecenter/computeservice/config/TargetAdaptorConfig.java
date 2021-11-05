package nl.esciencecenter.computeservice.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.esciencecenter.xenon.credentials.Credential;

public class TargetAdaptorConfig extends AdaptorConfig {
	@JsonProperty(value="hosted", required=false)
	private boolean hosted = false;
	
	public TargetAdaptorConfig() {
		super(null);
	}
	
	public TargetAdaptorConfig(Credential credential) {
		super(credential);
	}

	public boolean isHosted() {
		return hosted;
	}

	public void setHosted(boolean hosted) {
		this.hosted = hosted;
	}
}

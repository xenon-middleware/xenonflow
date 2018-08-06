package nl.esciencecenter.computeservice.config;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.esciencecenter.xenon.credentials.Credential;

public class TargetAdaptorConfig extends AdaptorConfig {
	@JsonProperty(value="baseurl", required=false)
	private String baseurl = "output";

	@JsonProperty(value="hosted", required=false)
	private boolean hosted = false;
	
	public TargetAdaptorConfig() {
		super(null);
	}
	
	public TargetAdaptorConfig(Credential credential) {
		super(credential);
	}

	@NotNull
	public String getBaseurl() {
		return baseurl;
	}

	public void setBaseurl(String baseurl) {
		if (baseurl == null) {
			this.baseurl = "output";
		} else {
			this.baseurl = baseurl;
		}
	}

	public boolean isHosted() {
		return hosted;
	}

	public void setHosted(boolean hosted) {
		this.hosted = hosted;
	}
}

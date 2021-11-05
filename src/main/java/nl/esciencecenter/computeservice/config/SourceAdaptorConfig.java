package nl.esciencecenter.computeservice.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.esciencecenter.xenon.credentials.Credential;

public class SourceAdaptorConfig extends AdaptorConfig {
	@JsonProperty(value="clearOnJobDone", required=false)
	private boolean clearOnJobDone = false;
	
	public SourceAdaptorConfig() {
		super(null);
	}
	
	public SourceAdaptorConfig(Credential credential) {
		super(credential);
	}

	public boolean shouldClearOnJobDone() {
		return clearOnJobDone;
	}

	public void setClearOnJobDone(boolean clearOnJobDone) {
		this.clearOnJobDone = clearOnJobDone;
	}
}

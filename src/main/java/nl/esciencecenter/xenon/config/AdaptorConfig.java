package nl.esciencecenter.xenon.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.credentials.DefaultCredential;

public class AdaptorConfig {
	private String adaptor;
	private String location;
	private Map<String, String> options;
	private Credential credential;
	
	public AdaptorConfig(@JsonProperty("credential") Credential credential) {
		if (credential == null){
			this.credential = new DefaultCredential();
		} else {
			this.credential = credential;
		}
	}
	
	public String getAdaptor() {
		return adaptor;
	}
	
	public void setAdaptor(String adaptor) {
		this.adaptor = adaptor;
	}
	
	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
	
	public Map<String, String> getOptions() {
		return options;
	}
	
	public void setOptions(Map<String, String> options) {
		this.options = options;
	}
	
	public Credential getCredential() {
		return credential;
	}
	
	public void setCredential(Credential credential) {
		this.credential = credential;
	}
}

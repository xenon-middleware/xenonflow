package nl.esciencecenter.xenon.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XenonConfig {
	private Map<String, ComputeResource> resources;
	
	public XenonConfig(@JsonProperty("ComputeResources") Map<String, ComputeResource> resources) {
		super();
		this.resources = resources;
	}
	
	public Map<String, ComputeResource> getResources() {
		return resources;
	}

	public void setResources(Map<String, ComputeResource> resources) {
		this.resources = resources;
	}
}

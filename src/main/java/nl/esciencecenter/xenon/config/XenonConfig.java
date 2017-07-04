package nl.esciencecenter.xenon.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XenonConfig {
	private Map<String, ComputeResource> computeResources;
	
	public XenonConfig(@JsonProperty("ComputeResources") Map<String, ComputeResource> computeResources) {
		super();
		this.computeResources = computeResources;
	}
	
	public Map<String, ComputeResource> getComputeResources() {
		return computeResources;
	}

	public void setComputeResources(Map<String, ComputeResource> computeResources) {
		this.computeResources = computeResources;
	}
}

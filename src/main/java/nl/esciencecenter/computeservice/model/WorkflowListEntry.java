package nl.esciencecenter.computeservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkflowListEntry {
	@JsonProperty("filename")
	String filename;

	@JsonProperty("path")
	String path;
	
	public WorkflowListEntry(
			@JsonProperty("filename") String filename,
			@JsonProperty("path") String path) {
		super();
		this.filename = filename;
		this.path = path;
	}
}

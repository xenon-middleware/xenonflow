package org.commonwl.cwl;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Step extends HashMap<String, Object> {
	private static final long serialVersionUID = 4240257742680645608L;

	private String id;
	private RunCommand run;

	public Step(
			@JsonProperty("id") String id,
			@JsonProperty("run")RunCommand run){
		this.id = id;
		this.run = run;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public RunCommand getRun() {
		return run;
	}

	public void setRun(RunCommand run) {
		this.run = run;
	}
	
	@Override
	public String toString() {
		return "Step [id=" + id + ", run=" + run + "]";
	}
}

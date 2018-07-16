package org.commonwl.cwl.types;

import org.commonwl.cwl.Parameter;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommandInputArraySchema extends Parameter {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8342076322361261649L;

	CommandInputArraySchema(
		@JsonProperty("id") String id,
		@JsonProperty("type") String type
	) {
		super(id, type);
	}

}

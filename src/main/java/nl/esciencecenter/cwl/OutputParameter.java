package nl.esciencecenter.cwl;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OutputParameter extends Parameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1477836903369041963L;
	
	public OutputParameter(
			@JsonProperty("id") String id,
			@JsonProperty("type") String type,
			@JsonProperty("label") String label
			){
		super(id, type, label);
	}

}

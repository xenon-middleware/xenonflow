package nl.esciencecenter.cwl;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InputParameter extends Parameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3096344382428378961L;

	public InputParameter(
			@JsonProperty("id") String id,
			@JsonProperty("type") String type,
			@JsonProperty("label") String label
			){
		super(id, type, label);
	}
}

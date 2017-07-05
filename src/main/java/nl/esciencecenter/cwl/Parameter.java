package nl.esciencecenter.cwl;

import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Parameter extends HashMap<String, Object> {
	private static final long serialVersionUID = -4237293788994574555L;

	private String id;
	private String type;
	private String label;
	
	public Parameter(
			@JsonProperty("id") String id,
			@JsonProperty("type") String type,
			@JsonProperty("label") String label
			){
		this.id = id;
		this.type = type;
		this.label = label;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
}

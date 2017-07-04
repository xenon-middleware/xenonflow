package nl.esciencecenter.cwl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class Workflow extends HashMap<String, Object> {
	private static final long serialVersionUID = -2170400805154307949L;
	private InputParameter[] inputs;
	private OutputParameter[] outputs;
	
	public static Workflow fromFile(File file) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());		
		
		Workflow wf = mapper.readValue(file, Workflow.class);
		
		return wf;
	}
	
	public Workflow(
			@JsonProperty("inputs") InputParameter[] inputs,
			@JsonProperty("outputs") OutputParameter[] outputs
			){
		super();
		this.setInputs(inputs);
		this.setOutputs(outputs);
	}

	public InputParameter[] getInputs() {
		return inputs;
	}

	public void setInputs(InputParameter[] inputs) {
		this.inputs = inputs;
	}

	public OutputParameter[] getOutputs() {
		return outputs;
	}

	public void setOutputs(OutputParameter[] outputs) {
		this.outputs = outputs;
	}
}

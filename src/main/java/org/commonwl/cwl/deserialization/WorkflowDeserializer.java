package org.commonwl.cwl.deserialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.commonwl.cwl.InputParameter;
import org.commonwl.cwl.OutputParameter;
import org.commonwl.cwl.Process;
import org.commonwl.cwl.Process.ProcessType;
import org.commonwl.cwl.RunCommand;
import org.commonwl.cwl.Step;
import org.commonwl.cwl.Workflow;
import org.commonwl.cwl.utils.IteratorUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WorkflowDeserializer extends JsonDeserializer<Workflow> {
	public static Logger logger = LoggerFactory.getLogger(WorkflowDeserializer.class);

	@Override
	public Workflow deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);
		ObjectMapper mapper = (ObjectMapper)p.getCodec(); 
		
		LinkedList<InputParameter> inputs = new LinkedList<InputParameter>();
		LinkedList<OutputParameter> outputs = new LinkedList<OutputParameter>();
		LinkedList<Step> steps = new LinkedList<Step>();

		if(node.has("inputs")) {
			JsonNode inputsNode = node.get("inputs");
			if (inputsNode.isArray()) {
				for (JsonNode inputNode : IteratorUtils.iterable(inputsNode.elements())) {
					String id = inputNode.get("id").asText();
					InputParameter input = deserializeInputParameter(id, inputNode, mapper);
					inputs.add(input);
				}
			} else {
				for (Entry<String, JsonNode> inputNode : IteratorUtils.iterable(inputsNode.fields())) {
					InputParameter input = deserializeInputParameter(inputNode.getKey(), inputNode.getValue(), mapper);
					 inputs.add(input);
				}
			}
		}
		
		if(node.has("outputs")) {
			JsonNode outputsNode = node.get("outputs");
			if (outputsNode.isArray()) {
				for (JsonNode outputNode : IteratorUtils.iterable(outputsNode.elements())) {
					String id = outputNode.get("id").asText();
					OutputParameter output = deserializeOutputParameter(id, outputNode, mapper);
					outputs.add(output);
				}
			} else {
				for (Entry<String, JsonNode> outputNode : IteratorUtils.iterable(outputsNode.fields())) {
					OutputParameter output = deserializeOutputParameter(outputNode.getKey(), outputNode.getValue(), mapper);
					outputs.add(output);
				}
			}
		}
		
		ProcessType type = ProcessType.CommandLineTool;
		if(node.has("class") && node.get("class").asText().equals("Workflow")) {
			type = ProcessType.Workflow;
			JsonNode stepsNode = node.get("steps");
			if (stepsNode.isArray()) {
				for (JsonNode stepNode : IteratorUtils.iterable(stepsNode.elements())) {
					String id = stepNode.get("id").asText();
					Step step = deserializeStep(id, stepNode, oc);
					steps.add(step);
				}
			} else {
				for (Entry<String, JsonNode> stepNode : IteratorUtils.iterable(stepsNode.fields())) {
					Step step = deserializeStep(stepNode.getKey(), stepNode.getValue(), oc);
					steps.add(step);
				}
			}
				
		}

		Workflow workflow = new Workflow(inputs.toArray(new InputParameter[inputs.size()]),
										 outputs.toArray(new OutputParameter[outputs.size()]),
										 steps.toArray(new Step[steps.size()]));
		
		workflow.setType(type);

		return workflow;
	}
	
	private Step deserializeStep(String id, JsonNode node, ObjectCodec oc) throws JsonProcessingException {
		JsonNode runNode = node.get("run");
		
		RunCommand s = new RunCommand();
		
		if (runNode.isTextual()) {
			s.setWorkflowPath(runNode.asText());
		} else {
			Process subProcess = oc.treeToValue(node, Process.class);
			s.setSubWorkflow(subProcess);
		}
		return new Step(id, s);
	}

	
	private InputParameter deserializeInputParameter(String id, JsonNode node, ObjectMapper mapper)
			throws JsonParseException, JsonMappingException, IOException {
		
		if (node.isTextual()) {
			return new InputParameter(id, node.asText());
		} else {
			TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
			JavaType hashmapType = mapper.getTypeFactory().constructType(typeRef);
			
			HashMap<String, Object> parameter = mapper.readValue(mapper.treeAsTokens(node), hashmapType);
			
			
			String type = inferType(node, parameter);
			InputParameter i = new InputParameter(id, type, isOptional(type));
			i.putAll(parameter);
			return i;
		}
	}
	
	private OutputParameter deserializeOutputParameter(String id, JsonNode node, ObjectMapper mapper) 
			throws JsonParseException, JsonMappingException, IOException {
		if (node.isTextual()) {
			return new OutputParameter(id, node.asText());
		} else {
			TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
			JavaType hashmapType = mapper.getTypeFactory().constructType(typeRef);
			
			HashMap<String, Object> parameter = mapper.readValue(mapper.treeAsTokens(node), hashmapType);
			
			
			String type = inferType(node, parameter);
			OutputParameter o = new OutputParameter(id, type, isOptional(type));
			o.putAll(parameter);
			return o;
		}
	}
	
	private String inferType(JsonNode node, HashMap<String, Object> parameter) {	
		String type = "unknown";
		try {
			type = (String)parameter.get("type");
		} catch(ClassCastException e) {
			type = "complex";
			JsonNode typeDef = node.findValue("type");
			
			if (typeDef.isObject()
					&& typeDef.get("type").isTextual()
					&& typeDef.get("type").asText().equals("array")) {
				JsonNode arrayType = typeDef.get("items");
				if (arrayType.isTextual() && arrayType.asText().equals("File")) {
					type = "File[]";
				} else if (arrayType.isTextual() && arrayType.asText().equals("Directory")) {
					type = "Directory[]";
				} else {
					logger.warn("Unrecognized array element type, no staging will be done on this input:\n" + arrayType.toString());
				}
			} else {
				logger.warn("Unrecognized complex input parameter type, no staging will be done on this input:\n" + typeDef.toString());
			}
		}
		return type;
	}
	
	private boolean isOptional(String type) {
		return type.endsWith("?");
	}
}

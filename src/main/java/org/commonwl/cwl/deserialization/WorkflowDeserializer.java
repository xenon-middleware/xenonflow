package org.commonwl.cwl.deserialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.commonwl.cwl.InputParameter;
import org.commonwl.cwl.OutputParameter;
import org.commonwl.cwl.Process;
import org.commonwl.cwl.Step;
import org.commonwl.cwl.Workflow;
import org.commonwl.cwl.RunCommand;
import org.commonwl.cwl.Process.ProcessType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WorkflowDeserializer extends JsonDeserializer<Workflow> {

	@Override
	public Workflow deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);
		ObjectMapper mapper = (ObjectMapper)p.getCodec();
		
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {}; 
		
		LinkedList<InputParameter> inputs = new LinkedList<InputParameter>();
		LinkedList<OutputParameter> outputs = new LinkedList<OutputParameter>();
		LinkedList<Step> steps = new LinkedList<Step>();

		if(node.has("inputs")) {
			JsonNode inputsNode = node.get("inputs");
			if (inputsNode.isArray()) {
				Iterator<JsonNode> iter = inputsNode.elements();
				while (iter.hasNext()) {
					JsonNode inputNode = iter.next();
					InputParameter i = oc.treeToValue(inputNode, InputParameter.class);
					inputs.add(i);
				}
			} else {
				Iterator<Entry<String, JsonNode> > iter = inputsNode.fields();
				while (iter.hasNext()) {
					Entry<String, JsonNode> inputNode = iter.next();
					
					if (inputNode.getValue().isTextual()) {
						InputParameter i = new InputParameter(inputNode.getKey(), (String)inputNode.getValue().asText());
						inputs.add(i);
					} else {
						HashMap<String, Object> parameter = mapper.readValue(mapper.treeAsTokens(inputNode.getValue()),
					 		 											 mapper.getTypeFactory().constructType(typeRef));
						
						String type = "unknown";
						try {
							type = (String)parameter.get("type");
						} catch(ClassCastException e) {
							type = "complex";
						}
						
						InputParameter i = new InputParameter(inputNode.getKey(), type);
						i.putAll(parameter);
						inputs.add(i);
					}
				}
			}
		}
		
		if(node.has("outputs")) {
			JsonNode outputsNode = node.get("outputs");
			if (outputsNode.isArray()) {
				Iterator<JsonNode> iter = outputsNode.elements();
				while (iter.hasNext()) {
					JsonNode outputNode = iter.next();
					OutputParameter o = oc.treeToValue(outputNode, OutputParameter.class);
					outputs.add(o);
				}
			} else {
				Iterator<Entry<String, JsonNode> > iter = outputsNode.fields();
				while (iter.hasNext()) {
					Entry<String, JsonNode> outputNode = iter.next();
					
					if (outputNode.getValue().isTextual()) {
						OutputParameter o = new OutputParameter(outputNode.getKey(), (String)outputNode.getValue().asText());
						outputs.add(o);
					} else {
						HashMap<String, Object> parameter = mapper.readValue(mapper.treeAsTokens(outputNode.getValue()),
										 							 		 mapper.getTypeFactory().constructType(typeRef));
						
						String type = "unknown";
						try {
							type = (String)parameter.get("type");
						} catch(ClassCastException e) {
							type = "complex";
						}
						
						OutputParameter o = new OutputParameter(outputNode.getKey(), type);
						o.putAll(parameter);
						outputs.add(o);
					}
				}
			}
		}
		
		ProcessType type = ProcessType.CommandLineTool;
		if(node.has("class") && node.get("class").asText().equals("Workflow")) {
			type = ProcessType.Workflow;
			JsonNode stepsNode = node.get("steps");
			if (stepsNode.isArray()) {
				Iterator<JsonNode> iter = stepsNode.elements();
				while (iter.hasNext()) {
					JsonNode stepNode = iter.next();
					JsonNode runNode = stepNode.get("run");
					
					RunCommand s = new RunCommand();
					
					if (runNode.isTextual()) {
						s.setWorkflowPath(runNode.asText());
					} else {
						Process subProcess = oc.treeToValue(stepNode, Process.class);
						s.setSubWorkflow(subProcess);
					}
					Step step = new Step(stepNode.get("id").asText(), s);
					steps.add(step);
				}
			} else {
				Iterator<Entry<String, JsonNode> > iter = stepsNode.fields();
				while (iter.hasNext()) {
					Entry<String, JsonNode> stepNode = iter.next();
					JsonNode runNode = stepNode.getValue().get("run");
					
					RunCommand s = new RunCommand();
					if (runNode.isTextual()) {
						s.setWorkflowPath(runNode.asText());
					} else {
						Process subProcess = oc.treeToValue(stepNode.getValue(), Process.class);
						s.setSubWorkflow(subProcess);
					}
					Step step = new Step(stepNode.getKey(), s);
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
}

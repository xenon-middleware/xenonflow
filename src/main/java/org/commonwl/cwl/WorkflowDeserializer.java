package org.commonwl.cwl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

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
					
					HashMap<String, Object> parameter = mapper.readValue(mapper.treeAsTokens(inputNode.getValue()),
					 		 											 mapper.getTypeFactory().constructType(typeRef));
										
					InputParameter i = new InputParameter(inputNode.getKey(), (String)parameter.get("type"));
					i.putAll(parameter);
					inputs.add(i);
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
					
					HashMap<String, Object> parameter = mapper.readValue(mapper.treeAsTokens(outputNode.getValue()),
									 							 		 mapper.getTypeFactory().constructType(typeRef));
					OutputParameter o = new OutputParameter(outputNode.getKey(), (String)parameter.get("type"));
					o.putAll(parameter);
					outputs.add(o);
				}
			}
		}
		
		Workflow workflow = new Workflow(inputs.toArray(new InputParameter[inputs.size()]),
										 outputs.toArray(new OutputParameter[outputs.size()]));
		
		return workflow;
	}
}

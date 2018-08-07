package org.commonwl.cwl.deserialization;

import java.io.IOException;
import java.util.Iterator;

import org.commonwl.cwl.Process;
import org.commonwl.cwl.Workflow;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class ProcessDeserializer extends JsonDeserializer<Process> {
	@Override
	public Process deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);
		
		if(node.has("$graph")) {
			JsonNode n = node.get("$graph");
			
			Iterator<JsonNode> nodes = n.elements();
			JsonNode main = null;
			while (nodes.hasNext()) {
				JsonNode m = nodes.next();
				if (m.get("id").asText().equals("#main")) {
					main = m;
				}
			}
			
			return oc.treeToValue(main, Workflow.class);
		} else {
			return oc.treeToValue(node, Workflow.class);
		}
	}
}

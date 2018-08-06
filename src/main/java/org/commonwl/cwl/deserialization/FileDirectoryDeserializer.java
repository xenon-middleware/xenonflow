package org.commonwl.cwl.deserialization;

import java.io.IOException;

import org.commonwl.cwl.DirEntry;
import org.commonwl.cwl.FileDirEntry;
import org.commonwl.cwl.FileEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class FileDirectoryDeserializer extends JsonDeserializer<FileDirEntry> {
	public static Logger logger = LoggerFactory.getLogger(FileDirectoryDeserializer.class);
	
	@Override
	public FileDirEntry deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p); 
		
		String entryClass = node.get("class").asText();
		if (entryClass.equals("File")) {
			return (FileDirEntry) oc.treeToValue(node, FileEntry.class);
		} else if (entryClass.equals("Directory")) {
			return (FileDirEntry) oc.treeToValue(node, DirEntry.class);
		} else {
			throw new IOException("Could not parse file or directory entry with class: " + entryClass + " expected either File or Directory");
		}
	}
}

package org.commonwl.cwl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.io.FilenameUtils;
import org.commonwl.cwl.deserialization.ProcessDeserializer;
import org.commonwl.cwl.deserialization.WorkflowDeserializer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import nl.esciencecenter.computeservice.utils.JacksonUtils;

public abstract class Process extends HashMap<String, Object> {
	private static final long serialVersionUID = -277384177779924611L;
	
	public enum ProcessType {
		Workflow,
		CommandLineTool
	}
	
	public abstract ProcessType getType();
	
	public boolean isWorkflow() {
		return this.getType() == ProcessType.Workflow;
	}

	public static Process fromFile(File file) throws JsonParseException, JsonMappingException, IOException {
		String extension = FilenameUtils.getExtension(file.getName());
		
		ObjectMapper mapper = JacksonUtils.getMapperForFileType(extension);		
		SimpleModule module = new SimpleModule();
		
		module.addDeserializer(Process.class, new ProcessDeserializer());
		module.addDeserializer(Workflow.class, new WorkflowDeserializer());
		mapper.registerModule(module);
		
		Process p = mapper.readValue(file, Process.class);
		
		return p;
	}
	
	public static Process fromInputStream(InputStream inputStream, String type) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = JacksonUtils.getMapperForFileType(type);
		SimpleModule module = new SimpleModule();
		
		module.addDeserializer(Process.class, new ProcessDeserializer());
		module.addDeserializer(Workflow.class, new WorkflowDeserializer());

		mapper.registerModule(module);
		Process p = mapper.readValue(inputStream, Process.class);
		
		return p;
	}
}

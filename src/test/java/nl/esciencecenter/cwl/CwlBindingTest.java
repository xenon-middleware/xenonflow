package nl.esciencecenter.cwl;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import nl.esciencecenter.cwl.InputParameter;
import nl.esciencecenter.cwl.Workflow;

public class CwlBindingTest {
	
	@Test
	public void inputBindingTest() throws JsonParseException, JsonMappingException, IOException{
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());		
		
		Workflow tool = mapper.readValue(new File("src/test/resources/cwl/echo.cwl"), Workflow.class);
		
		for(InputParameter input : tool.getInputs()){
			System.out.println(input.getId());
		}
	}

}

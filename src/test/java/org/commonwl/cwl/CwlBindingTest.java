/**
 * Copyright 2013 Netherlands eScience Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonwl.cwl;

import java.io.File;
import java.io.IOException;

import org.commonwl.cwl.InputParameter;
import org.commonwl.cwl.Workflow;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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

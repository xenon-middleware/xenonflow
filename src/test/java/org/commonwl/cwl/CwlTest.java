/**
w * Copyright 2013 Netherlands eScience Center
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.commonwl.cwl.Process.ProcessType;
import org.commonwl.cwl.utils.CWLUtils;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class CwlTest {

	@Test
	public void inputBindingTest() throws JsonParseException, JsonMappingException, IOException {
		Workflow tool =  Workflow.fromFile(new File("src/test/resources/cwl/echo.cwl"));

		assertEquals("First input should be inp", "inp", tool.getInputs()[0].getId());
	}

	@Test
	public void workflowTypeTest() throws JsonParseException, JsonMappingException, IOException {
		Workflow workflow = Workflow.fromFile(new File("src/test/resources/cwl/count-lines.cwl"));
		
		assertEquals("Workflow type of count-lines.cwl should be Workflow", ProcessType.Workflow, workflow.getType()); 
	}
	
	@Test
	public void commandLineToolTypeTest() throws JsonParseException, JsonMappingException, IOException {
		Workflow workflow = Workflow.fromFile(new File("src/test/resources/cwl/wc-tool.cwl"));
		
		assertEquals("Workflow type of wc-tool.cwl should be CommandLineTool", ProcessType.CommandLineTool, workflow.getType()); 
	}
	
	@Test
	public void workflowStepTest() throws JsonParseException, JsonMappingException, IOException {
		Workflow workflow = Workflow.fromFile(new File("src/test/resources/cwl/count-lines.cwl"));
		
		Step firstStep = workflow.getSteps()[0];
		RunCommand actualStep = firstStep.getRun();
		
		assertEquals("Workflow type of count-lines.cwl should be Workflow", "step1", firstStep.getId());
		assertEquals("Workflow type of count-lines.cwl should be Workflow", "wc-tool.cwl", actualStep.getWorkflowPath());
	}
	
	@Test
	public void workflowPathTest() throws JsonParseException, JsonMappingException, IOException {
		Workflow workflow = Workflow.fromFile(new File("src/test/resources/cwl/count-lines.cwl"));
		
		List<String> expected = Arrays.asList("wc-tool.cwl", "parseInt-tool.cwl");
		
		List<String> paths = CWLUtils.getWorkflowPaths(workflow);
		
		assertEquals("Expected 2 local paths in count-lines.cwl", expected, paths);
	}
	
//	@Test
//	public void localWorkflowPathTest() throws JsonParseException, JsonMappingException, IOException {
//		Workflow workflow = Workflow.fromFile(new File("src/test/resources/cwl/count-lines-remote.cwl"));
//		
//		List<Path> expected = Arrays.asList(new Path("parseInt-tool.cwl"));
//		
//		List<Path> paths = CWLUtils.getLocalWorkflowPaths(workflow, new Path("cwl"), fileSystem, Logger jobLogger);
//		
//		assertEquals("Expected 1 local path in count-lines-remote.cwl", expected, paths);
//	}
	
	@Test
	public void arraySchemaTest() throws JsonParseException, JsonMappingException, IOException {
		Workflow workflow = Workflow.fromFile(new File("src/test/resources/cwl/array-input.cwl"));
		
		assertEquals("Expected file array as input", "File[]", workflow.getInputs()[0].getType());
	}	
}

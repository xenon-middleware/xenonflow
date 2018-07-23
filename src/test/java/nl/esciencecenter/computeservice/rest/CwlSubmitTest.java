package nl.esciencecenter.computeservice.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.esciencecenter.client.CWLState;
import nl.esciencecenter.client.Job;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {nl.esciencecenter.computeservice.rest.Application.class})
public class CwlSubmitTest {
	@Autowired
    private MockMvc mockMvc;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Test
	public void getJobsTest() throws Exception {
		this.mockMvc.perform(get("/jobs")).andDo(print()).andExpect(status().isOk());
	}
	
	@Test
	public void submitAndWaitEchoTest() throws Exception {
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/echo-test.json")));
		
		Job job = postJobAndWaitForFinal(contents);
			
		assertTrue(job.getState() == CWLState.SUCCESS);
	}
	
	@Test
	public void submitAndWaitCopyTest() throws Exception {	
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/copy-test.json")));
		Job job = postJobAndWaitForFinal(contents);
		CWLState state = job.getState();
		
		assertTrue(state == CWLState.SUCCESS);
		
		assertTrue(job.getOutput().containsKey("out"));
		@SuppressWarnings("unchecked")
		HashMap<String, Object> out = (HashMap<String, Object>) job.getOutput().get("out");
		
		assertEquals(job.getId() + "/ipsum.txt", (String)out.get("path"));
	}
	
	@Test
	public void submitAndWaitEchoFailTest() throws Exception {	
		String contents = new String("{\"name\":\"echo-fail\",\"workflow\":\"echo.cwl\",\"input\":{}}");
		Job job = postJobAndWaitForFinal(contents);
		CWLState state = job.getState();
		
		assertTrue(state.isErrorState());
	}
	
	@Test
	public void submitAndCancelImmediatelySleepTest() throws Exception {	
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/sleep-test.json")));

		MockHttpServletResponse response = postJob(contents);
		
		String location = response.getHeader("location");
		this.mockMvc.perform(post(location+"/cancel")).andExpect(status().is2xxSuccessful());
				
		Job job = waitForFinal(location);
		
		assertTrue(job.getState() == CWLState.CANCELLED);
	}
	
	@Test
	public void submitAndCancelSleepTest() throws Exception {
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/sleep-test.json")));
		
		MockHttpServletResponse response = postJob(contents);
		String location = response.getHeader("location");
				
		Job job = waitForRunning(location);
		this.mockMvc.perform(post(location+"/cancel")).andExpect(status().is2xxSuccessful());
		
		job = waitForFinal(location);
		assertTrue(job.getState() == CWLState.CANCELLED);
	}
	
	@Test
	public void submitAndDeleteEchoTest() throws Exception {
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/echo-test.json")));
		
		Job job = postJobAndWaitForFinal(contents);
		String location = job.getUri();
		this.mockMvc.perform(delete(location)).andExpect(status().is2xxSuccessful());
		
		this.mockMvc.perform(get(location)).andExpect(status().isNotFound());
	}

	
	private MockHttpServletResponse postJob(String contents) throws Exception {
		return this.mockMvc.perform(post("/jobs")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(contents)
		).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
	}
	
	private Job waitForFinal(String location) throws Exception {
		CWLState state = CWLState.WAITING;
		while(!state.isFinal()) {
			MockHttpServletResponse res = this.mockMvc.perform(get(location)).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
			Job job = mapper.readValue(res.getContentAsString(), Job.class);
			state = job.getState();
			
			if (state.isFinal()) {
				 return job;
			}
			Thread.sleep(1000);
		}
		return null;
	}
	
	private Job waitForRunning(String location) throws Exception {
		CWLState state = CWLState.WAITING;
		while(!state.isRunning()) {
			MockHttpServletResponse res = this.mockMvc.perform(get(location)).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
			Job job = mapper.readValue(res.getContentAsString(), Job.class);
			state = job.getState();
			
			if (state.isRunning()) {
				 return job;
			}
			Thread.sleep(1000);
		}
		return null;
	}
	
	private Job postJobAndWaitForFinal(String contents)
			throws Exception {
		
		MockHttpServletResponse response = postJob(contents);
		
		String location = response.getHeader("location");
		
		return waitForFinal(location);
	}
}

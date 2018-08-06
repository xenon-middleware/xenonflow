package nl.esciencecenter.computeservice.utils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.esciencecenter.client.CWLState;
import nl.esciencecenter.client.Job;

public class CwlTestUtils {
	private static ObjectMapper mapper = new ObjectMapper();
	private static List<String> created = new LinkedList<String>();

	public static List<String> getCreated() {
		return created;
	}
	
	public static void clearCreated() {
		created.clear();
	}

	public static Job waitForStatus(String location, CWLState status, MockMvc mockMvc) throws Exception {
		CWLState state = null;
		Job job = null;
		while(state == null || !state.equals(status)) {
			MockHttpServletResponse res = mockMvc.perform(get(location)).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
			job = mapper.readValue(res.getContentAsString(), Job.class);
			state = job.getState();
			Thread.sleep(1000);
		}
		return job;
	}
	
	public static Job waitForFinal(String location, MockMvc mockMvc) throws Exception {
		CWLState state = CWLState.WAITING;
		Job job = null;
		while(!state.isFinal()) {
			MockHttpServletResponse res = mockMvc.perform(get(location)).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
			job = mapper.readValue(res.getContentAsString(), Job.class);
			state = job.getState();
			
			if (state.isFinal()) {
				 return job;
			}
			Thread.sleep(1000);
		}
		return job;
	}
	
	public static Job waitForRunning(String location, MockMvc mockMvc) throws Exception {
		CWLState state = CWLState.WAITING;
		while(!state.isRunning()) {
			MockHttpServletResponse res = mockMvc.perform(get(location)).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
			Job job = mapper.readValue(res.getContentAsString(), Job.class);
			state = job.getState();
			
			if (state.isRunning()) {
				 return job;
			}
			Thread.sleep(1000);
		}
		return null;
	}
	
	public static Job postJobAndWaitForFinal(String contents, MockMvc mockMvc)
			throws Exception {
		
		MockHttpServletResponse response = CwlTestUtils.postJob(contents, mockMvc);
		
		String location = response.getHeader("location");
		
		return CwlTestUtils.waitForFinal(location, mockMvc);
	}
	
	public static MockHttpServletResponse postJob(String contents, MockMvc mockMvc) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(post("/jobs")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(contents)
		).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
		 
		Job job = mapper.readValue(response.getContentAsString(), Job.class);
		created.add(job.getId());
		
		return response;
	}
}

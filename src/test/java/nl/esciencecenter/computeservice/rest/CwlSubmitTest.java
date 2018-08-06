package nl.esciencecenter.computeservice.rest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import nl.esciencecenter.client.CWLState;
import nl.esciencecenter.client.Job;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.computeservice.utils.CwlTestUtils;
import nl.esciencecenter.xenon.filesystems.Path;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {nl.esciencecenter.computeservice.rest.Application.class})
@TestPropertySource(locations="classpath:test.properties")
public class CwlSubmitTest {
	Logger logger = LoggerFactory.getLogger(CwlSubmitTest.class);

	@Autowired
    private MockMvc mockMvc;
	
	@Autowired
	private XenonService xenonService;
	
	@After
	public void deleteJob() throws Exception {
		for (String jobId : CwlTestUtils.getCreated()) {
			this.mockMvc.perform(delete("/jobs/"+jobId));
		}
		CwlTestUtils.clearCreated();
	}
	
	@Test
	public void getJobsTest() {
		assertThatCode(() -> {
			this.mockMvc.perform(get("/jobs")).andExpect(status().isOk());
		}).doesNotThrowAnyException();
	}
	
	@Test
	public void submitAndWaitEchoTest() {
		logger.info("Starting echo test");
		assertThatCode(() -> {
			String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/echo-test.json")));
			Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc);
			
			assertTrue(job.getState() == CWLState.SUCCESS);
		}).doesNotThrowAnyException();
	}
	
	@Test
	public void submitAndWaitFailTest() {
		logger.info("Starting fail test");
		assertThatCode(() -> {
			String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/fail-test.json")));
			Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc);
			
			assertTrue(job.getState() == CWLState.PERMANENT_FAILURE);
		}).doesNotThrowAnyException();
	}
	
	@Test
	public void submitAndWaitLogTest() throws Exception {
		logger.info("Starting log test");
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/echo-test.json")));
		Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc);
		
		assertThatCode(() -> {
			this.mockMvc.perform(get(job.getLog())
					.accept(MediaType.TEXT_PLAIN)
			).andExpect(status().is2xxSuccessful());
		}).doesNotThrowAnyException();
	}
	
	@Test
	public void doesNotExistTest() throws Exception {
		logger.info("Starting does not exist test");
		assertThatCode(() -> {
			this.mockMvc.perform(get("/jobs/this_id_does_not_exist")
					.accept(MediaType.APPLICATION_JSON)
			).andExpect(status().is(404));
			
			this.mockMvc.perform(get("/jobs/this_id_does_not_exist/log")).andExpect(status().is(404));
			
			this.mockMvc.perform(post("/jobs/this_id_does_not_exist/cancel")).andExpect(status().is(404));
			this.mockMvc.perform(delete("/jobs/this_id_does_not_exist")).andExpect(status().is(404));
		}).doesNotThrowAnyException();
	}
	
	@Test
	public void noBodyTest() throws Exception {
		logger.info("Starting does not exist test");
		assertThatCode(() -> {
			
			String contents = new String("{}");
			mockMvc.perform(post("/jobs")
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON)
					.content(contents)).andExpect(status().is4xxClientError());
			
			contents = new String("{ \"name\": \"test\"}");
			mockMvc.perform(post("/jobs")
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON)
					.content(contents)).andExpect(status().is4xxClientError());
			
		}).doesNotThrowAnyException();
	}
	
	@Test
	public void submitAndWaitCopyTest() throws Exception {	
		logger.info("Starting copy test");
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/copy-test.json")));
		Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc);
		CWLState state = job.getState();
		
		assertTrue(state == CWLState.SUCCESS);
		
		assertTrue(job.getOutput().containsKey("out"));
		@SuppressWarnings("unchecked")
		HashMap<String, Object> out = (HashMap<String, Object>) job.getOutput().get("out");
		
		assertEquals(job.getSandboxDirectory() + "/ipsum.txt", (String)out.get("path"));
	}

	@Test
	public void submitAndWaitCopyDirectoryTest() throws Exception {	
		logger.info("Starting copy directory test");
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/copy-dir-test.json")));
		
		Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc);
		CWLState state = job.getState();
		
		assertTrue(state == CWLState.SUCCESS);
		
		assertTrue(job.getOutput().containsKey("out"));
		@SuppressWarnings("unchecked")
		HashMap<String, Object> out = (HashMap<String, Object>) job.getOutput().get("out");
		
		assertEquals(job.getSandboxDirectory() + "/output", (String)out.get("path"));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void submitAndWaitCopyDirectoryArrayTest() throws Exception {
		logger.info("Starting copy directory array test");
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/copy-dir-array-test.json")));
		Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc);
		CWLState state = job.getState();
		
		assertTrue(state == CWLState.SUCCESS);
		
		assertTrue(job.getOutput().containsKey("out"));
		List<Object> out = (List<Object>) job.getOutput().get("out");
		assertEquals(2, out.size());
		
		HashMap<String, Object> dir = (HashMap<String, Object>) out.get(0);
		assertTrue(dir.containsKey("class"));
		assertEquals("Directory", (String)dir.get("class"));
		assertTrue(dir.containsKey("location"));
		
		HashMap<String, Object> dir2 = (HashMap<String, Object>) out.get(1);
		assertTrue(dir2.containsKey("class"));
		assertEquals("Directory", (String)dir2.get("class"));
		assertTrue(dir2.containsKey("location"));
		
		assertTrue(xenonService.getTargetFileSystem().exists(new Path((String) dir.get("path"))));
		assertTrue(xenonService.getTargetFileSystem().exists(new Path((String) dir2.get("path"))));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void submitAndWaitCopyFileArrayTest() throws Exception {
		logger.info("Starting copy file array test");
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/copy-file-array-test.json")));
		Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc);
		CWLState state = job.getState();
		
		assertTrue(state == CWLState.SUCCESS);
		
		assertTrue(job.getOutput().containsKey("out"));
		List<Object> out = (List<Object>) job.getOutput().get("out");
		assertEquals(2, out.size());
		
		HashMap<String, Object> file = (HashMap<String, Object>) out.get(0);
		assertTrue(file.containsKey("class"));
		assertEquals("File", (String)file.get("class"));
		assertTrue(file.containsKey("location"));
		
		HashMap<String, Object> file2 = (HashMap<String, Object>) out.get(1);
		assertTrue(file2.containsKey("class"));
		assertEquals("File", (String)file2.get("class"));
		assertTrue(file2.containsKey("location"));
		
		assertTrue(xenonService.getTargetFileSystem().exists(new Path((String) file.get("path"))));
		assertTrue(xenonService.getTargetFileSystem().exists(new Path((String) file2.get("path"))));
	}
	
	@Test
	public void submitAndWaitEchoFailTest() throws Exception {	
		logger.info("Starting echo failure test");
		String contents = "{\"name\":\"echo-fail\",\"workflow\":\"echo.cwl\",\"input\":{}}";
		Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc);
		CWLState state = job.getState();
		
		assertTrue(state.isErrorState());
	}
	
	@Test
	public void submitAndCancelImmediatelySleepTest() throws Exception {
		logger.info("Starting cancel immediately test");
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/sleep-test.json")));

		MockHttpServletResponse response = CwlTestUtils.postJob(contents, mockMvc);
		
		String location = response.getHeader("location");
		this.mockMvc.perform(post(location+"/cancel")).andExpect(status().is2xxSuccessful());
				
		Job job = CwlTestUtils.waitForFinal(location, mockMvc);
		
		assertTrue(job.getState() == CWLState.CANCELLED);
	}
	
	@Test
	public void submitAndCancelSleepTest() throws Exception {
		
		logger.info("Starting cancel test");
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/sleep-test.json")));
		
		MockHttpServletResponse response = CwlTestUtils.postJob(contents, mockMvc);
		String location = response.getHeader("location");

		Job job = CwlTestUtils.waitForRunning(location, mockMvc);
		Thread.sleep(1000);
		this.mockMvc.perform(post(location+"/cancel")).andExpect(status().is2xxSuccessful());
		
		job = CwlTestUtils.waitForFinal(location, mockMvc);
		assertTrue(job.getState() == CWLState.CANCELLED);
	}
	
/* This one currently does not work. It needs mocking of XenonService so it puts
 * the second job in the internal WAITING state
 * @Test
	public void submitAndCancelWaitingSleepTest() throws Exception {
		logger.info("Starting cancel waiting test");
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/sleep-test.json")));
		
		MockHttpServletResponse response = postJob(contents);
		String location1 = response.getHeader("location");
		response = postJob(contents);
		String location2 = response.getHeader("location");
		
		Job job = CwlTestUtils.waitForRunning(location1);
		job = CwlTestUtils.waitForStatus(location2, CWLState.WAITING);

		this.mockMvc.perform(post(location2+"/cancel")).andExpect(status().is2xxSuccessful());
		
		assertTrue(job.getState() == CWLState.CANCELLED);
		job = waitForFinal(location1);
		assertTrue(job.getState() == CWLState.SUCCESS);
	}
*/
	@Test
	public void submitAndDeleteEchoTest() throws Exception {
		logger.info("Starting delete test");
		String contents = new String(Files.readAllBytes(Paths.get("src/test/resources/jobs/echo-test.json")));
		
		Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc);
		String location = job.getUri();
		this.mockMvc.perform(delete(location)).andExpect(status().is2xxSuccessful());
		
		this.mockMvc.perform(get(location)).andExpect(status().isNotFound());
	}
}

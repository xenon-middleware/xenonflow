package nl.esciencecenter.computeservice.rest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import nl.esciencecenter.client.Job;
import nl.esciencecenter.computeservice.utils.CwlTestUtils;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {nl.esciencecenter.computeservice.rest.Application.class})
@TestPropertySource(locations="classpath:test.properties")
public class CwlResultTest {
	Logger logger = LoggerFactory.getLogger(CwlResultTest.class);

	@Autowired
    private MockMvc mockMvc;
	
	@Value("${xenonflow.http.auth-token-header-name}")
	private String headerName;

	@Value("${xenonflow.http.auth-token}")
	private String apiToken;

	@AfterAll
	public void deleteJob() throws Exception {
		for (String jobId : CwlTestUtils.getCreated()) {
			this.mockMvc.perform(
					delete("/jobs/"+jobId)
					.header(headerName, apiToken)
				);
			Thread.sleep(1100);
		}
		CwlTestUtils.clearCreated();
	}
	
	@Test
	public void echoErrorLogTest() {
		logger.info("Starting echo error log test");
		assertThatCode(() -> {
			String contents = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("jobs/echo-test.json"), "UTF-8");
			Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc, headerName, apiToken);
			
			MockHttpServletResponse response = this.mockMvc.perform(
					get(job.getLog())
					.header(headerName, apiToken)
			).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
			assertFalse(response.getContentAsString().isEmpty());
		}).doesNotThrowAnyException();
	}
	
	@Test
	public void echoJobIdAndNameTest() {
		logger.info("Starting jobid and name test");
		assertThatCode(() -> {
			String contents = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("jobs/jobid-test.json"), "UTF-8");
			Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc, headerName, apiToken);
			
			MockHttpServletResponse response = this.mockMvc.perform(
					get(job.getLog())
					.header(headerName, apiToken)
			).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
			assertFalse(response.getContentAsString().isEmpty());
			
			String output = (String) job.getOutput().get("out");
			assertTrue(output.equals(job.getId() + " " + job.getName()+"\n"));
		}).doesNotThrowAnyException();
	}
}

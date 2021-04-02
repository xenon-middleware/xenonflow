package nl.esciencecenter.computeservice.rest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {nl.esciencecenter.computeservice.rest.Application.class})
@TestPropertySource(locations="classpath:test.properties")
public class WorkflowListTest {
	Logger logger = LoggerFactory.getLogger(CwlSubmitTest.class);

	@Autowired
    private MockMvc mockMvc;
	
	@Value("${xenonflow.http.auth-token-header-name}")
	private String headerName;

	@Value("${xenonflow.http.auth-token}")
	private String apiToken;
	
	@Test
	public void getJobsTest() {
		assertThatCode(() -> {
			this.mockMvc.perform(
					get("/workflows")
					.header(headerName, apiToken)
			).andExpect(status().isOk());
		}).doesNotThrowAnyException();
	}
	
}

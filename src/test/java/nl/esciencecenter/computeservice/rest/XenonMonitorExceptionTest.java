package nl.esciencecenter.computeservice.rest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.UUID;

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

import nl.esciencecenter.client.CWLState;
import nl.esciencecenter.client.Job;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.model.JobState;
import nl.esciencecenter.computeservice.model.StatePreconditionException;
import nl.esciencecenter.computeservice.model.WorkflowBinding;
import nl.esciencecenter.computeservice.utils.CwlTestUtils;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {nl.esciencecenter.computeservice.rest.Application.class})
@TestPropertySource(locations="classpath:test.properties")
public class XenonMonitorExceptionTest {
	Logger logger = LoggerFactory.getLogger(CwlSubmitTest.class);

	@Autowired
    private MockMvc mockMvc;
		
	@Autowired
	private JobRepository repository;
	
	@Value("${xenonflow.http.auth-token-header-name}")
	private String headerName;

	@Value("${xenonflow.http.auth-token}")
	private String apiToken;

	@Test
	public void jobDoesNotExistRunningTest() {
		assertThatCode(() -> {		
			nl.esciencecenter.computeservice.model.Job job = createJob();
			job.setInternalState(JobState.RUNNING);
			
			job = repository.save(job);
			Job job2 = CwlTestUtils.waitForFinal(job.getUri(), mockMvc, headerName, apiToken);
			
			assertEquals(CWLState.SYSTEM_ERROR, job2.getState());
		}).doesNotThrowAnyException();
	}
	
	@Test
	public void jobDoesNotExistRunninCRTest() {
		assertThatCode(() -> {		
			nl.esciencecenter.computeservice.model.Job job = createJob();
			job.setInternalState(JobState.RUNNING_CR);
			
			job = repository.save(job);
			Job job2 = CwlTestUtils.waitForFinal(job.getUri(), mockMvc, headerName, apiToken);
			
			assertEquals(CWLState.CANCELLED, job2.getState());
		}).doesNotThrowAnyException();
	}
	
	private nl.esciencecenter.computeservice.model.Job createJob() throws StatePreconditionException {
		nl.esciencecenter.computeservice.model.Job job = new nl.esciencecenter.computeservice.model.Job();
		String uuid = UUID.randomUUID().toString();
		
		job.setId(uuid);
		job.setInput(new WorkflowBinding());
		job.setName("does-not-exist");
		job.setWorkflow("something.cwl");
		job.setLog("/jobs/" + uuid + "/log");
		job.getAdditionalInfo().put("createdAt", new Date());
		job.setXenonId("NOTEXISTING-1");
		job.setURI("/jobs/"+uuid);
		
		return job;
	}
}

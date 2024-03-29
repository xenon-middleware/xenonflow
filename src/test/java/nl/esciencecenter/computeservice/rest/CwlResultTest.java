package nl.esciencecenter.computeservice.rest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
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

import nl.esciencecenter.client.CWLState;
import nl.esciencecenter.client.Job;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.computeservice.utils.CwlTestUtils;
import nl.esciencecenter.xenon.filesystems.CopyMode;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {nl.esciencecenter.computeservice.rest.Application.class})
@TestPropertySource(locations="classpath:test.properties")
public class CwlResultTest {
	Logger logger = LoggerFactory.getLogger(CwlResultTest.class);

	@Autowired
    private MockMvc mockMvc;
	
	@Autowired
	private XenonService xenonService;

	@Value("${xenonflow.http.auth-token-header-name}")
	private String headerName;

	@Value("${xenonflow.http.auth-token}")
	private String apiToken;

	@After
	public void deleteJob() throws Exception {
		for (Job job : CwlTestUtils.getCreated()) {
			mockMvc.perform(
					delete(job.getUri())
					.header(headerName, apiToken)
				);
			Thread.sleep(1100);
			
			FileSystem targetFileSystem = xenonService.getTargetFileSystem();
			Path targetPath = targetFileSystem.getWorkingDirectory().resolve(job.getSandboxDirectory());
			assertFalse(targetFileSystem.exists(targetPath));
		}
		CwlTestUtils.clearCreated();
	}
	
	@Test
	public void echoErrorLogTest() {
		logger.info("Starting echo error log test");
		assertThatCode(() -> {
			String contents = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("jobs/echo-test.json"), "UTF-8");
			Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc, headerName, apiToken);
			
			MockHttpServletResponse response = mockMvc.perform(
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
			
			MockHttpServletResponse response = mockMvc.perform(
					get(job.getLog())
					.header(headerName, apiToken)
			).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
			assertFalse(response.getContentAsString().isEmpty());
			
			String output = (String) job.getOutput().get("out");
			assertTrue(output.equals(job.getId() + " " + job.getName()+"\n"));
		}).doesNotThrowAnyException();
	}
	
	@Test
	public void testDeletingInput() {
		assertThatCode(() -> {
			xenonService.getConfig().getSourceFilesystemConfig().setClearOnJobDone(true);
			
			// Create an input directory and populate it with an input file.
			String uuid = UUID.randomUUID().toString();
			FileSystem inputFileSystem = xenonService.getSourceFileSystem();
			
			Path inputDir = inputFileSystem.getWorkingDirectory().resolve(uuid);
			inputFileSystem.createDirectory(inputDir);
			
			Path sourceFile = inputFileSystem.getWorkingDirectory().resolve("input").resolve("lorem.txt");
			Path inputFile = inputDir.resolve("lorem.txt");
			String copyId = inputFileSystem.copy(sourceFile, inputFileSystem, inputFile, CopyMode.IGNORE, false);
			inputFileSystem.waitUntilDone(copyId, 1000);
			
			
			String contents = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("jobs/copy-test.json"), "UTF-8");
			contents = contents.replace("input/", uuid+"/");
			
			Job job = CwlTestUtils.postJobAndWaitForFinal(contents, mockMvc, headerName, apiToken);
			assertTrue(job.getState() == CWLState.SUCCESS);
			
			assertFalse(inputFileSystem.exists(inputFile));
			
			inputFileSystem.delete(inputDir, true);
			
			xenonService.getConfig().getSourceFilesystemConfig().setClearOnJobDone(false);
		}).doesNotThrowAnyException();
	}
}

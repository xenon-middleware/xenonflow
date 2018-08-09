package nl.esciencecenter.computeservice.rest;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.xenon.schedulers.JobDescription;
import nl.esciencecenter.xenon.schedulers.Scheduler;
import nl.esciencecenter.xenon.schedulers.Streams;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {nl.esciencecenter.computeservice.rest.Application.class})
@TestPropertySource(locations="classpath:test.properties")
public class XenonTest {
	@Autowired
	private XenonService xenonService;
	
	@Test
	public void xenonEnvironmentTest() {
		assertThatCode(() -> {	
			Scheduler scheduler = xenonService.getScheduler();
			
			JobDescription description = new JobDescription();
			description.setExecutable("env");
			description.setMaxRuntime(15);
			description.setWorkingDirectory(xenonService.getSourceFileSystem().getWorkingDirectory().toString());
			
			Streams s = scheduler.submitInteractiveJob(description);
			
			System.out.write(s.getStdout().readAllBytes());
		}).doesNotThrowAnyException();
	}
}

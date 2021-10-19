package nl.esciencecenter.computeservice.rest.service.tasks;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.commonwl.cwl.CwlException;
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

import com.fasterxml.jackson.databind.JsonMappingException;

import nl.esciencecenter.computeservice.config.AdaptorConfig;
import nl.esciencecenter.computeservice.config.XenonflowConfig;
import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.model.JobState;
import nl.esciencecenter.computeservice.model.StatePreconditionException;
import nl.esciencecenter.computeservice.model.WorkflowBinding;
import nl.esciencecenter.computeservice.model.XenonflowException;
import nl.esciencecenter.computeservice.service.JobService;
import nl.esciencecenter.computeservice.service.staging.CwlFileStagingObject;
import nl.esciencecenter.computeservice.service.staging.FileStagingObject;
import nl.esciencecenter.computeservice.service.staging.StagingManifest;
import nl.esciencecenter.computeservice.service.staging.StagingManifestFactory;
import nl.esciencecenter.computeservice.service.staging.StagingObject;
import nl.esciencecenter.computeservice.service.staging.StringToFileStagingObject;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {nl.esciencecenter.computeservice.rest.Application.class})
@TestPropertySource(locations="classpath:test.properties")
public class CwlStageInTest {
	@Value("${xenonflow.config}")
	private String xenonConfigFile;
	
	@Autowired
	private JobService jobService;
	
	@Autowired
	private JobRepository repository;
	
	private FileSystem sourceFileSystem;
	
	private FileSystem getSourceFileSystem() throws XenonException, IOException {
		if (sourceFileSystem == null || !sourceFileSystem.isOpen()) {
			
			String xenonflowHome = System.getenv("XENONFLOW_HOME");
			String xenonflowFiles = System.getenv("XENONFLOW_FILES");
			
			if (xenonflowHome == null) {
				xenonflowHome = Paths.get(".").toAbsolutePath().normalize().toString();
			}
			if (xenonflowFiles == null) {
				xenonflowFiles = xenonflowHome;
			}
			
			System.out.println("Loading config from:" + xenonConfigFile);
			XenonflowConfig config = XenonflowConfig.loadFromFile(xenonConfigFile, xenonflowHome, xenonflowFiles);
			
			// Initialize local filesystem
			AdaptorConfig sourceConfig = config.getSourceFilesystemConfig();
			sourceFileSystem = FileSystem.create(sourceConfig.getAdaptor(), sourceConfig.getLocation(),
					sourceConfig.getCredential(), sourceConfig.getProperties());
		}
		return sourceFileSystem;
	}

	@Test
	public void createStagingManifestTest() throws JsonMappingException, IOException, StatePreconditionException, CwlException, XenonflowException, XenonException {
		String uuid = UUID.randomUUID().toString();

		Logger jobLogger = LoggerFactory.getLogger("jobs." + uuid);

		Job job = new Job();
		job.setId(uuid);
		job.setInput(WorkflowBinding.fromFile(new File("src/test/resources/cwl/echo-file.json")));
		job.setName("createStagingManifestTest");
		job.setInternalState(JobState.SUCCESS);
		job.setWorkflow("echo-file.cwl");
		job.setURI("");
		job.setLog("");
		
		
		repository.save(job);
		
		StagingManifest manifest = StagingManifestFactory.createStagingInManifest(job, this.getSourceFileSystem(), this.getSourceFileSystem(), null, jobLogger, jobService);
		
		List<String> paths = new ArrayList<String>();
		for (StagingObject stageObject : manifest) {
			if (stageObject instanceof CwlFileStagingObject) {
				CwlFileStagingObject object = (CwlFileStagingObject) stageObject;
				paths.add(object.getTargetPath().toString());
			} else if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				paths.add(object.getTargetPath().toString());
			} else if (stageObject instanceof StringToFileStagingObject) {
				StringToFileStagingObject object = (StringToFileStagingObject) stageObject;
				paths.add(object.getTargetPath().toString());
			}
		}
		
		repository.delete(job);
		
		List<String> expected = Arrays.asList("cwlcommand", "echo-file.cwl", "echo-file.json", "job-order.json");
		assertEquals("Expecting arrays to be equal", expected, paths);
	}
	
	@Test
	public void createStagingManifestMultiFileTest() throws JsonMappingException, IOException, StatePreconditionException, CwlException, XenonflowException, XenonException {
		String uuid = UUID.randomUUID().toString();

		Logger jobLogger = LoggerFactory.getLogger("jobs." + uuid);

		Job job = new Job();
		job.setId(uuid);
		job.setWorkflow("count-lines-remote.cwl");
		job.setInput(WorkflowBinding.fromFile(new File("src/test/resources/cwl/count-lines-job.json")));
		job.setName("createStagingManifestTest");
		job.setInternalState(JobState.SUCCESS);
		job.setURI("");
		job.setLog("");
		
		repository.saveAndFlush(job);
		
		StagingManifest manifest = StagingManifestFactory.createStagingInManifest(job, this.getSourceFileSystem(), this.getSourceFileSystem(), null, jobLogger, jobService);
		
		List<String> paths = new ArrayList<String>();
		for (StagingObject stageObject : manifest) {
			if (stageObject instanceof CwlFileStagingObject) {
				CwlFileStagingObject object = (CwlFileStagingObject) stageObject;
				paths.add(object.getTargetPath().toString());
			} else if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				paths.add(object.getTargetPath().toString());
			} else if (stageObject instanceof StringToFileStagingObject) {
				StringToFileStagingObject object = (StringToFileStagingObject) stageObject;
				paths.add(object.getTargetPath().toString());
			}
		}
		
		repository.delete(job);
		
		List<String> expected = Arrays.asList("cwlcommand", "count-lines-remote.cwl", "parseInt-tool.cwl", "ipsum.txt", "job-order.json");
		assertEquals("Expecting arrays to be equal", expected, paths);
	}
}

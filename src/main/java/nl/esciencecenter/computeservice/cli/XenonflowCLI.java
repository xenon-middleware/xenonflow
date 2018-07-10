package nl.esciencecenter.computeservice.cli;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.service.JobService;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.computeservice.service.staging.XenonStager;
import nl.esciencecenter.xenon.XenonException;

@Configuration
@EnableAutoConfiguration
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = {"nl.esciencecenter.computeservice.service*", "nl.esciencecenter.computeservice.model*"})
@EntityScan(basePackages = {"nl.esciencecenter.computeservice.service*", "nl.esciencecenter.computeservice.model*", "nl.esciencecenter.computeservice.cwl*" })
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"nl.esciencecenter.computeservice.service*", "nl.esciencecenter.computeservice.model*", "nl.esciencecenter.computeservice.cwl*" })
public class XenonflowCLI implements CommandLineRunner {

	@Autowired
	XenonService xenonService;
	
	@Autowired
	private JobRepository repository;
	
	@Value("${xenon.config}")
	private String xenonConfigFile;

	@Bean
	public static ThreadPoolTaskScheduler taskScheduler() {
		final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		return scheduler;
	}
	
	@Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();
        return executor;
    }
	
	@Bean
	public static XenonStager sourceToRemoteStager(XenonService xenonService, JobRepository repository, JobService jobService) throws XenonException {
		return new XenonStager(jobService, repository, xenonService.getSourceFileSystem(), xenonService.getRemoteFileSystem(), xenonService);
	}
	
	@Bean
	public static XenonStager remoteToTargetStager(XenonService xenonService, JobRepository repository, JobService jobService) throws XenonException {
		return new XenonStager(jobService, repository, xenonService.getTargetFileSystem(), xenonService.getRemoteFileSystem(), xenonService);
	}
	
	public JCommander initialize(String...arguments) {
		JCommander jc = JCommander.newBuilder()
			.programName("xenonflow-admin")
			.addObject(new CommonParameters())
			.addCommand("list", new CommandList())
			.addCommand("clear-database", new CommandClearDatabase())
			.build();
		
		try {
			jc.parse(arguments);
		} catch (ParameterException e) {
			e.usage();
			System.exit(1);
		}
		
		return jc;
	}

	public int doCommand(JCommander arguments) {
		if (arguments == null || arguments.getParsedCommand() == null) {
			arguments.usage();
			System.exit(1);
		}
		switch (arguments.getParsedCommand()) {
			case "list":
				List<Job> jobs = repository.findAll();
				for (Job job : jobs) {
					System.out.println(job.getId() + " (" + job.getName() + ")");
				}
				break;
			case "clear-database":
				repository.deleteAll();
				break;
			default:
				arguments.usage();
				break;
		}
		return 0;
	}
	
	public static void main(String[] args) throws Exception {
		System.setProperty("spring.devtools.livereload.enabled", "false");
		new SpringApplicationBuilder(XenonflowCLI.class).logStartupInfo(false).web(false).run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		JCommander arguments = initialize(args);
		int exitcode = doCommand(arguments);
		System.exit(exitcode);
	}
}

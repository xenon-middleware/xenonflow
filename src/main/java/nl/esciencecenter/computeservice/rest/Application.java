package nl.esciencecenter.computeservice.rest;

import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import nl.esciencecenter.computeservice.config.ComputeServiceConfig;
import nl.esciencecenter.computeservice.config.TargetAdaptorConfig;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.service.JobService;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.computeservice.service.staging.XenonStager;
import nl.esciencecenter.xenon.XenonException;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableAutoConfiguration
@EnableSwagger2
@EnableScheduling
@EnableAsync
@EnableWebSecurity
@ComponentScan(basePackages = {"nl.esciencecenter.computeservice.rest*", "nl.esciencecenter.computeservice.model*", "nl.esciencecenter.computeservice.service*"})
@EntityScan(basePackages = { "nl.esciencecenter.computeservice.rest*", "nl.esciencecenter.computeservice.cwl*", "nl.esciencecenter.computeservice.model*", "nl.esciencecenter.computeservice.service*"})
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = { "nl.esciencecenter.computeservice.model*", "nl.esciencecenter.computeservice.cwl*" })
@Order(1)
public class Application extends WebSecurityConfigurerAdapter implements WebMvcConfigurer, CommandLineRunner, ApplicationListener<ApplicationReadyEvent> {
	private static final Logger logger = LoggerFactory.getLogger(Application.class);
	
	@Value("${local.server.address}")
	private String bindAdress;
	
	@Value("${xenon.config}")
	private String xenonConfigFile;
	
	@Value("${xenonflow.http.auth-token-header-name}")
    private String principalRequestHeader;

    @Value("${xenonflow.http.auth-token}")
    private String principalRequestValue;

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        APIKeyAuthFilter filter = new APIKeyAuthFilter(principalRequestHeader);
        filter.setAuthenticationManager(new AuthenticationManager() {

            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String principal = (String) authentication.getPrincipal();
                if (!principalRequestValue.equals(principal))
                {
                    throw new BadCredentialsException("The API key was not found or not the expected value.");
                }
                authentication.setAuthenticated(true);
                return authentication;
            }
        });
        httpSecurity.
            antMatcher("/**").
            csrf().disable().
            sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).
            and().addFilter(filter).authorizeRequests().anyRequest().authenticated();
    }
	
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
		return new XenonStager(jobService, repository, xenonService.getRemoteFileSystem(), xenonService.getTargetFileSystem(), xenonService);
	}
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String xenonflowHome = System.getenv("XENONFLOW_HOME");
		
		if (xenonflowHome == null) {
			xenonflowHome = Paths.get(".").toAbsolutePath().normalize().toString();
		}
		ComputeServiceConfig config;
		try {
			config = ComputeServiceConfig.loadFromFile(xenonConfigFile, xenonflowHome);
			TargetAdaptorConfig targetConfig = config.getTargetFilesystemConfig();
			
			if (targetConfig.isHosted() && targetConfig.getAdaptor().equals("file")) {
				String resourceLocation = targetConfig.getBaseurl() + "/**";
				logger.info("Adding resource location handler for: " + resourceLocation);
				registry.addResourceHandler(resourceLocation).addResourceLocations("file:" + targetConfig.getLocation());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		registry.addResourceHandler("swagger-ui.html")
				.addResourceLocations("classpath:/META-INF/resources/");

		registry.addResourceHandler("/webjars/**")
		        .addResourceLocations("classpath:/META-INF/resources/webjars/");
	}

	@Override
	public void run(String... arg0) throws Exception {
		if (arg0.length > 0 && arg0[0].equals("exitcode")) {
			throw new ExitException();
		}
	}

	public static void main(String[] args) throws Exception {
		new SpringApplication(Application.class).run(args);
	}

	class ExitException extends RuntimeException implements ExitCodeGenerator {
		private static final long serialVersionUID = 1L;

		@Override
		public int getExitCode() {
			return 10;
		}

	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		logger.info("Server running at: http://" + bindAdress);
	}
}

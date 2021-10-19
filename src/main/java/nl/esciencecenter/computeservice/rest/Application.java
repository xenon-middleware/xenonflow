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
import org.springframework.http.HttpMethod;
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
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import nl.esciencecenter.computeservice.config.XenonflowConfig;
import nl.esciencecenter.computeservice.config.TargetAdaptorConfig;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.service.JobService;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.computeservice.service.staging.RemoteToTargetStager;
import nl.esciencecenter.computeservice.service.staging.SourceToRemoteStager;
import nl.esciencecenter.computeservice.service.staging.XenonStager;
import nl.esciencecenter.xenon.XenonException;

@Configuration
@EnableAutoConfiguration
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
	
	@Value("${server.port}")
	private String serverPort;
	
	@Value("${server.ssl.enabled}")
	private boolean ssl;
	
	@Value("${xenonflow.config}")
	private String xenonConfigFile;
	
	@Value("${xenonflow.http.auth-token-header-name}")
    private String principalRequestHeader;

    @Value("${xenonflow.http.auth-token}")
    private String principalRequestValue;
    
    @Value("${xenonflow.admin.location}")
    private String adminLocation;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**");
    }
 
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
            sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and().addFilter(filter)
            .authorizeRequests()
            	.antMatchers(HttpMethod.OPTIONS,"/jobs").permitAll()//allow CORS option calls
            	.antMatchers(HttpMethod.HEAD,"/jobs").permitAll() //allow CORS option calls
            	.antMatchers(HttpMethod.OPTIONS,"/jobs/**").permitAll()//allow CORS option calls
            	.antMatchers(HttpMethod.HEAD,"/jobs/**").permitAll() //allow CORS option calls
            	.antMatchers(HttpMethod.OPTIONS,"/files/**").permitAll()//allow CORS option calls
            	.antMatchers(HttpMethod.HEAD,"/files/**").permitAll() //allow CORS option calls
            	.antMatchers(HttpMethod.OPTIONS,"/output/**").permitAll()//allow CORS option calls
            	.antMatchers(HttpMethod.HEAD,"/output/**").permitAll() //allow CORS option calls
            	.antMatchers(HttpMethod.OPTIONS,"/status/**").permitAll()//allow CORS option calls
            	.antMatchers(HttpMethod.HEAD,"/status/**").permitAll() //allow CORS option calls
            	.antMatchers(HttpMethod.OPTIONS,"/workflows/**").permitAll()//allow CORS option calls
            	.antMatchers(HttpMethod.HEAD,"/workflows/**").permitAll() //allow CORS option calls
            	.antMatchers("/jobs").authenticated()
            	.antMatchers("/jobs/**").authenticated()
            	.antMatchers("/files").authenticated()
            	.antMatchers("/files/**").authenticated()
            	.antMatchers("/output").authenticated()
            	.antMatchers("/output/**").authenticated()
            	.antMatchers("/status").authenticated()
            	.antMatchers("/status/**").authenticated()
            	.antMatchers("/workflows").authenticated()
            	.antMatchers("/workflows/**").authenticated()
        		.antMatchers("/**").permitAll();
    }
	
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(64000);
        return loggingFilter;
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
		return new SourceToRemoteStager(jobService, repository, xenonService);
	}
	
	@Bean
	public static XenonStager remoteToTargetStager(XenonService xenonService, JobRepository repository, JobService jobService) throws XenonException {
		return new RemoteToTargetStager(jobService, repository, xenonService);
	}
	
	@Bean
	public TargetAdaptorConfig targetFileSystemConfig() {
		String xenonflowHome = System.getenv("XENONFLOW_HOME");
		String xenonflowFiles = System.getenv("XENONFLOW_FILES");
		
		if (xenonflowHome == null) {
			xenonflowHome = Paths.get(".").toAbsolutePath().normalize().toString();
		}
		if (xenonflowFiles == null) {
			xenonflowFiles = xenonflowHome;
		}
		XenonflowConfig config;
		try {
			config = XenonflowConfig.loadFromFile(xenonConfigFile, xenonflowHome, xenonflowFiles);
			return config.getTargetFilesystemConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		TargetAdaptorConfig targetConfig = targetFileSystemConfig(); 
		if (targetConfig.isHosted() && targetConfig.getAdaptor().equals("file")) {
			String resourceLocation = "/output/**";
			logger.info("Adding resource location handler for: " + resourceLocation);
			registry.addResourceHandler(resourceLocation)
					.addResourceLocations("file:" + targetConfig.getLocation());
		}
		
		registry.addResourceHandler("/swagger/**")
				.addResourceLocations("classpath:/META-INF/resources/");

		registry.addResourceHandler("/webjars/**")
		        .addResourceLocations("classpath:/META-INF/resources/webjars/");
		
		registry.addResourceHandler("/admin/**")
			.addResourceLocations(adminLocation);
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
		String scheme = ssl ? "https://" : "http://";
		logger.info("Server running at: " + scheme + bindAdress + ":" + serverPort);
	}
}

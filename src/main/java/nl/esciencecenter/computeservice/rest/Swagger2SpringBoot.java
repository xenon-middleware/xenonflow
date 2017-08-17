package nl.esciencecenter.computeservice.rest;

import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.servlets.WebdavServlet;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
@ComponentScan("nl.esciencecenter.computeservice.rest*")
@EntityScan(basePackages = {"nl.esciencecenter.computeservice.rest*", "nl.esciencecenter.computeservice.cwl.*"})
@EnableJpaRepositories(basePackages = {"nl.esciencecenter.computeservice.rest*", "nl.esciencecenter.computeservice.cwl.*"})
@EnableAsync
public class Swagger2SpringBoot implements CommandLineRunner {
	@Bean
	public static ServletRegistrationBean servletRegistrationBean(){

		final ServletRegistrationBean registration = new ServletRegistrationBean(new WebdavServlet());

		final Map<String, String> params = new HashMap<String, String>();
		params.put("debug", "1");
		params.put("listings", "true");
		params.put("readonly", "false");
		registration.setInitParameters(params);
		registration.addUrlMappings("/webdav/*");

		registration.setLoadOnStartup(1);

		return registration;
	}
	
	@Bean
    public static ThreadPoolTaskScheduler taskScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        return scheduler;
    }

	@Override
	public void run(String... arg0) throws Exception {
		if (arg0.length > 0 && arg0[0].equals("exitcode")) {
			throw new ExitException();
		}
	}

	public static void main(String[] args) throws Exception {
		new SpringApplication(Swagger2SpringBoot.class).run(args);
	}

	class ExitException extends RuntimeException implements ExitCodeGenerator {
		private static final long serialVersionUID = 1L;

		@Override
		public int getExitCode() {
			return 10;
		}

	}
}

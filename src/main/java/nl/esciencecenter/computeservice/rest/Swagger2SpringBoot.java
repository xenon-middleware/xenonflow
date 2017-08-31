package nl.esciencecenter.computeservice.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.servlets.WebdavServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableAutoConfiguration
@EnableSwagger2
@ComponentScan("nl.esciencecenter.computeservice.rest*")
@EntityScan(basePackages = { "nl.esciencecenter.computeservice.rest*", "nl.esciencecenter.computeservice.cwl.*" })
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = { "nl.esciencecenter.computeservice.rest*",
		"nl.esciencecenter.computeservice.cwl.*" })
public class Swagger2SpringBoot implements CommandLineRunner, ApplicationListener<EmbeddedServletContainerInitializedEvent> {
	private static final Logger logger = LoggerFactory.getLogger(Swagger2SpringBoot.class);

// I can't get the CORS for the embedded webdav to work, so it's not much sense
// to include it.
//	@Bean
//	public static ServletRegistrationBean servletRegistrationBean() {
//
//		final ServletRegistrationBean registration = new ServletRegistrationBean(new WebdavServlet());
//
//		final Map<String, String> params = new HashMap<String, String>();
//		params.put("debug", "1");
//		params.put("listings", "true");
//		params.put("readonly", "false");
//		
//		params.put("cors.allowed.origins", "*");
//
//		registration.setInitParameters(params);
//		registration.addUrlMappings("/webdav/*");		
//		registration.setLoadOnStartup(1);
//		
//		return registration;
//	}
	
	@Value("${server.address}")
	String bindAdress;

	@Bean
	public static ThreadPoolTaskScheduler taskScheduler() {
		final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(5);
		return scheduler;
	}
	
	@Override
	public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
    	int port = event.getEmbeddedServletContainer().getPort();
		logger.info("Server running at: http://" + bindAdress + ":" + port);
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

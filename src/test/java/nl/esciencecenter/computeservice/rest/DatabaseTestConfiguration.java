package nl.esciencecenter.computeservice.rest;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {"nl.esciencecenter.computeservice.rest*", "nl.esciencecenter.computeservice.cwl.*"})
@EnableJpaRepositories(basePackages = {"nl.esciencecenter.computeservice.rest*", "nl.esciencecenter.computeservice.cwl.*"})
public class DatabaseTestConfiguration {
	
}

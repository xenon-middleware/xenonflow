package nl.esciencecenter.computeservice.rest.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.annotations.ApiParam;
import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobDescription;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.model.StatePreconditionException;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.xenon.XenonException;

@CrossOrigin
@Controller
public class JobsApiController implements JobsApi {
	private static final Logger logger = LoggerFactory.getLogger(JobsApi.class);

	@Autowired
	XenonService xenonService;
	
	@Autowired
	JobRepository repository;
	
	@Override
	public ResponseEntity<Job> cancelJobById(@ApiParam(value = "Job ID",required=true ) @PathVariable("jobId") String jobId) {
		Job job;
		try {
			job = xenonService.cancelJob(jobId);
			if (job != null) {
				HttpHeaders headers = new HttpHeaders();
				ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequestUri();
				builder.replacePath("/jobs/" + job.getId());
				
				logger.debug("Setting location header to: " + builder.build().toUri());
				headers.setLocation(builder.build().toUri());
				
				return new ResponseEntity<Job>(job, headers, HttpStatus.OK);
			}
			return new ResponseEntity<Job>(HttpStatus.NOT_FOUND);
		} catch (StatePreconditionException e) {
			logger.error("Error during job cancellation request:", e);
			return new ResponseEntity<Job>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		
	}

	@Override
	public ResponseEntity<Void> deleteJobById(@ApiParam(value = "Job ID",required=true ) @PathVariable("jobId") String jobId) {
		try {
			Job job = xenonService.deleteJob(jobId);
			if (job != null) {
				HttpHeaders headers = new HttpHeaders();
				ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequestUri();
				
				logger.debug("Setting location header to: " + builder.build().toUri());
				headers.setLocation(builder.replacePath("/jobs").build().toUri());
				
				return new ResponseEntity<Void>(headers, HttpStatus.OK);
			}
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		} catch (XenonException | StatePreconditionException e) {
			// TODO Auto-generated catch block
			logger.error("Error job deletion request: ", e);
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<Job> getJobById(@ApiParam(value = "Job ID",required=true ) @PathVariable("jobId") String jobId) {
		Job job = repository.findOne(jobId);
		if (job == null) {
			return new ResponseEntity<Job>(HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<Job>(job, HttpStatus.OK);
		}
	}

	@Override
	public ResponseEntity<Object> getJobLogById(@ApiParam(value = "Job ID",required=true ) @PathVariable("jobId") String jobId) {
		logger.debug("JobId: "+jobId);
		String logFileName = xenonService.getJobLogName("jobs."+jobId);
		logger.debug("Loading log file from: " + logFileName);
		
		File logFile = new File(logFileName);
		InputStreamResource inputStreamResource;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentLength(logFile.length());
			inputStreamResource = new InputStreamResource(new FileInputStream(logFile));
			return new ResponseEntity<Object>(inputStreamResource, headers, HttpStatus.OK);
		} catch (FileNotFoundException e) {
			return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
		}
	}

	@Override
	public ResponseEntity<List<Job>> getJobs() {
		return new ResponseEntity<List<Job>>(repository.findAll(), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Job> postJob(@ApiParam(value = "Input binding for workflow." ,required=true ) @RequestBody JobDescription body) {
		try {
			Job job = xenonService.submitJob(body);
			
			HttpHeaders headers = new HttpHeaders();
			ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequestUri();
			builder.pathSegment(job.getId());
			
			logger.debug("Setting location header to: " + builder.build().toUri());
			headers.setLocation(builder.build().toUri());
			
			return new ResponseEntity<Job>(job, headers, HttpStatus.CREATED);
		} catch (XenonException | StatePreconditionException e) {
			logger.error("Error while posting job", e);
			Job job = new Job();
			job.getAdditionalInfo().put("exception", e);
			return new ResponseEntity<Job>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}

package nl.esciencecenter.computeservice.rest.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobDescription;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.model.Job.StateEnum;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.Path;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.*;

@Controller
public class JobsApiController implements JobsApi {
	private static final Logger logger = LoggerFactory.getLogger(JobsApi.class);

	@Autowired
	XenonService xenonService;
	
	@Autowired
	JobRepository repository;
	
	@Override
	public ResponseEntity<Job> cancelJobById(@ApiParam(value = "Job ID",required=true ) @PathVariable("jobId") String jobId) {
		// TODO Auto-generated method stub
		return JobsApi.super.cancelJobById(jobId);
	}

	@Override
	public ResponseEntity<Void> deleteJobById(@ApiParam(value = "Job ID",required=true ) @PathVariable("jobId") String jobId) {
		// TODO Auto-generated method stub
		return JobsApi.super.deleteJobById(jobId);
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
		} catch (XenonException e) {
			logger.error("Error while posting job", e);
		}
		return new ResponseEntity<Job>(HttpStatus.INTERNAL_SERVER_ERROR);
	}
}

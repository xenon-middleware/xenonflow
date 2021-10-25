package nl.esciencecenter.computeservice.rest.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.commonwl.cwl.utils.CWLUtils;
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
import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.JobDescription;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.model.JobState;
import nl.esciencecenter.computeservice.model.StatePreconditionException;
import nl.esciencecenter.computeservice.service.JobService;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.computeservice.service.tasks.DeleteJobTask;
import nl.esciencecenter.computeservice.utils.InetUtils;
import nl.esciencecenter.computeservice.utils.LoggingUtils;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.Path;

@CrossOrigin
@Controller
public class JobsApiController implements JobsApi {
	private static final Logger logger = LoggerFactory.getLogger(JobsApiController.class);
	private static final Logger requestLogger = LoggerFactory.getLogger("requests");

	@Autowired
	private XenonService xenonService;

	@Autowired
	private JobRepository repository;

	@Autowired
	private JobService jobService;

	@Autowired
	private DeleteJobTask deleteJobTask;

	@Override
	public ResponseEntity<Job> cancelJobById(@ApiParam(value = "Job ID",required=true ) @PathVariable("jobId") String jobId) {
		requestLogger.info("CANCEL request received for job: " + jobId);
		Optional<Job> job;
		try {
			job = cancelJob(jobId);
			if (job.isPresent()) {
				HttpHeaders headers = new HttpHeaders();
				ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequestUri();
				builder.replacePath("/jobs/" + job.get().getId());

				logger.debug("Setting location header to: " + builder.build().toUri());
				headers.setLocation(builder.build().toUri());

				return new ResponseEntity<Job>(job.get(), headers, HttpStatus.OK);
			}
			return new ResponseEntity<Job>(HttpStatus.NOT_FOUND);
		} catch (StatePreconditionException e) {
			logger.error("Error during job cancellation request:", e);
			return new ResponseEntity<Job>(HttpStatus.INTERNAL_SERVER_ERROR);
		}


	}

	@Override
	public ResponseEntity<Void> deleteJobById(@ApiParam(value = "Job ID",required=true ) @PathVariable("jobId") String jobId) {
		requestLogger.info("DELETE request received for job: " + jobId);
		try {
			Optional<Job> j = deleteJob(jobId);
			if (j.isPresent()) {
				HttpHeaders headers = new HttpHeaders();
				ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequestUri();

				logger.debug("Setting location header to: " + builder.build().toUri());
				headers.setLocation(builder.replacePath("/jobs").build().toUri());

				return new ResponseEntity<Void>(headers, HttpStatus.OK);
			}
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		} catch (StatePreconditionException e) {
			logger.error("Error job deletion request: ", e);
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<Job> getJobById(@ApiParam(value = "Job ID",required=true ) @PathVariable("jobId") String jobId) {
		requestLogger.info("GET request received for job: " + jobId);
		Optional<Job> job = repository.findById(jobId);
		if (job.isPresent()) {
			return new ResponseEntity<Job>(job.get(), HttpStatus.OK);
		} else {
			return new ResponseEntity<Job>(HttpStatus.NOT_FOUND);
		}
	}

	@Override
	public ResponseEntity<Object> getJobLogById(@ApiParam(value = "Job ID",required=true ) @PathVariable("jobId") String jobId) {
		requestLogger.info("JOBLOG request received for job: " + jobId);

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
		} catch (Exception e) {
			return new ResponseEntity<Object>(e, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<List<Job>> getJobs(HttpServletRequest request) {
		requestLogger.info("GET all jobs request received from: " + InetUtils.getClientIpAddr(request));
		return new ResponseEntity<List<Job>>(repository.findAll(), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Job> postJob(@ApiParam(value = "Input binding for workflow." ,required=true ) @RequestBody JobDescription body) {
		requestLogger.info("POST request received with description: " + body);
		try {
			String uuid = UUID.randomUUID().toString();

			if (body.getName() == null || body.getWorkflow() == null) {
				Job job = new Job();
				job.setId(uuid);
				job.setName(uuid);
				job.setWorkflow("none");
				job.setInternalState(JobState.PERMANENT_FAILURE);

				job.getAdditionalInfo().put("error", "Name and Workflow cannot be null");
				return new ResponseEntity<Job>(job, HttpStatus.BAD_REQUEST);
			}
			
			String workflow = body.getWorkflow();
			if (workflow.contains("#")) {
				workflow = workflow.split("#")[0];
			}

			if(!CWLUtils.isLocalWorkflow(new Path(workflow), xenonService.getCwlFileSystem())) {
				Job job = new Job();
				job.setId(uuid);
				job.setName(body.getName());
				job.setWorkflow(body.getWorkflow());
				job.setInternalState(JobState.PERMANENT_FAILURE);

				job.getAdditionalInfo().put("error", "supplied workflow is not an exisiting workflow");
				return new ResponseEntity<Job>(job, HttpStatus.BAD_REQUEST);
			}

			Job job = submitJob(body, uuid);

			HttpHeaders headers = new HttpHeaders();
			ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequestUri();
			builder.pathSegment(job.getId());

			logger.debug("Setting location header to: " + builder.build().toUri());
			headers.setLocation(builder.build().toUri());

			return new ResponseEntity<Job>(job, headers, HttpStatus.CREATED);
		} catch (StatePreconditionException | XenonException e) {
			logger.error("Error while posting job", e);
			Job job = new Job();
			job.getAdditionalInfo().put("exception", e);
			return new ResponseEntity<Job>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public Job submitJob(JobDescription body, String uuid) throws StatePreconditionException {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + uuid);
		LoggingUtils.addFileAppenderToLogger("jobs." + uuid, xenonService.getJobLogName("jobs." + uuid));

		Job job = new Job();
		job.setId(uuid);
		job.setInput(body.getInput());
		job.setName(body.getName());
		job.setInternalState(JobState.SUBMITTED);
		job.setWorkflow(body.getWorkflow());

		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequest();
		builder.pathSegment(job.getId());
		job.setURI(builder.build().toString());

		builder.pathSegment("log");
		job.setLog(builder.build().toString());

		ServletUriComponentsBuilder b = ServletUriComponentsBuilder.fromCurrentRequest();
		String baseurl = b.path("/..").build().normalize().toString();
		job.getAdditionalInfo().put("baseurl", baseurl);
		job.getAdditionalInfo().put("createdAt", new Date());

		job = repository.saveAndFlush(job);

		jobLogger.info("Submitted Job: " + job);

		return job;
	}

	public Optional<Job> cancelJob(String jobId) throws StatePreconditionException {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + jobId);

		jobLogger.info("Trying to cancel job " + jobId);

		Optional<Job> j = repository.findById(jobId);
		if (j.isPresent()) {
			Job job = j.get();
			if(!job.getInternalState().isFinal()) {
				switch (job.getInternalState()) {
					case STAGING_IN:
						job = jobService.setJobState(jobId, JobState.STAGING_IN, JobState.STAGING_IN_CR);
						break;
					case WAITING:
						job = jobService.setJobState(jobId, JobState.WAITING, JobState.WAITING_CR);
						break;
					case RUNNING:
						job = jobService.setJobState(jobId, JobState.RUNNING, JobState.RUNNING_CR);
						break;
					case STAGING_OUT:
						job = jobService.setJobState(jobId, JobState.STAGING_OUT, JobState.STAGING_OUT_CR);
						break;
					default:
						if (!job.getInternalState().isFinal()) {
							job = jobService.setJobState(jobId, job.getInternalState(), JobState.CANCELLED);
						}
						break;
				}
			}
			return Optional.ofNullable(job);
		}
		return j;
	}

	public Optional<Job> deleteJob(String jobId) throws StatePreconditionException {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + jobId);

		jobLogger.info("Going to delete job " + jobId);

		Optional<Job> j = repository.findById(jobId);
		if (j.isPresent()) {
			Job job = j.get();
			switch (job.getInternalState()) {
				case STAGING_IN:
					job = jobService.setJobState(jobId, JobState.STAGING_IN, JobState.STAGING_IN_DELR);
					break;
				case WAITING:
					job = jobService.setJobState(jobId, JobState.WAITING, JobState.WAITING_DELR);
					break;
				case RUNNING:
					job = jobService.setJobState(jobId, JobState.RUNNING, JobState.RUNNING_DELR);
					break;
				case STAGING_OUT:
					job = jobService.setJobState(jobId, JobState.STAGING_OUT, JobState.STAGING_OUT_DELR);
					break;
				default:
					deleteJobTask.deleteJob(jobId);
					break;
			}
			return Optional.ofNullable(job);
		}
		return j;
	}
}

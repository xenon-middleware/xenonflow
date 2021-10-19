package nl.esciencecenter.computeservice.rest.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.JobDescription;

@Api(value = "jobs")
public interface JobsApi {

	@ApiOperation(value = "Cancel a job", notes = "", response = Job.class, tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Job has been cancelled if job was still running or waiting", response = Job.class),
			@ApiResponse(code = 404, message = "Job not found", response = Job.class) })
	@RequestMapping(value = "/jobs/{jobId}/cancel", method = RequestMethod.POST)
	ResponseEntity<Job> cancelJobById(
			@ApiParam(value = "Job ID", required = true) @PathVariable("jobId") String jobId);

	@ApiOperation(value = "Deleta a job", notes = "Delete a job, if job is in waiting or running state then job will be cancelled first.", response = Void.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 204, message = "Job deleted", response = Void.class),
			@ApiResponse(code = 404, message = "Job not found", response = Void.class) })
	@RequestMapping(value = "/jobs/{jobId}", method = RequestMethod.DELETE)
	ResponseEntity<Void> deleteJobById(
			@ApiParam(value = "Job ID", required = true) @PathVariable("jobId") String jobId);

	@ApiOperation(value = "Get a job", notes = "", response = Job.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Status of job", response = Job.class),
			@ApiResponse(code = 404, message = "Job not found", response = Job.class) })
	@RequestMapping(value = "/jobs/{jobId}", produces = { "application/json" }, method = RequestMethod.GET)
	ResponseEntity<Job> getJobById(
			@ApiParam(value = "Job ID", required = true) @PathVariable("jobId") String jobId);

	@ApiOperation(value = "Log of a job", notes = "", response = String.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Job log", response = String.class),
			@ApiResponse(code = 302, message = "Job log redirect", response = String.class),
			@ApiResponse(code = 404, message = "Job not found", response = String.class) })
	@RequestMapping(value = "/jobs/{jobId}/log", produces = { "text/plain" }, method = RequestMethod.GET)
	ResponseEntity<Object> getJobLogById(
			@ApiParam(value = "Job ID", required = true) @PathVariable("jobId") String jobId);

	@ApiOperation(value = "list of jobs", notes = "get a list of all jobs, running, cancelled, or otherwise.", response = Job.class, responseContainer = "List", tags = {})
	@ApiResponses(value = { @ApiResponse(code = 200, message = "list of jobs", response = Job.class) })
	@RequestMapping(value = "/jobs", produces = { "application/json" }, method = RequestMethod.GET)
	ResponseEntity<List<Job>> getJobs(HttpServletRequest request); // {


	@ApiOperation(value = "submit a new job", notes = "Submit a new job from a workflow definition.", response = Job.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 201, message = "OK", response = Job.class) })
	@RequestMapping(value = "/jobs", produces = { "application/json" }, consumes = {
			"application/json" }, method = RequestMethod.POST)
	ResponseEntity<Job> postJob(
			@ApiParam(value = "Input binding for workflow.", required = true) @RequestBody JobDescription body); // {

}

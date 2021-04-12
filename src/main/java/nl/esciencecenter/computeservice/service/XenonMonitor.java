package nl.esciencecenter.computeservice.service;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.model.JobState;
import nl.esciencecenter.computeservice.model.StatePreconditionException;
import nl.esciencecenter.computeservice.service.staging.XenonStager;
import nl.esciencecenter.computeservice.service.tasks.CwlStageInTask;
import nl.esciencecenter.computeservice.service.tasks.CwlStageOutTask;
import nl.esciencecenter.computeservice.service.tasks.CwlWorkflowTask;
import nl.esciencecenter.computeservice.service.tasks.DeleteJobTask;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.adaptors.schedulers.JobCanceledException;
import nl.esciencecenter.xenon.schedulers.JobStatus;
import nl.esciencecenter.xenon.schedulers.NoSuchJobException;
import nl.esciencecenter.xenon.schedulers.Scheduler;

@Component
public class XenonMonitor {
	private static final Logger logger = LoggerFactory.getLogger(XenonMonitor.class);

	@Autowired
	private XenonService xenonService;
	
	@Autowired
	private JobRepository repository;
	
	@Autowired
	private JobService jobService;
	
	@Autowired
	private XenonStager sourceToRemoteStager;
	
	@Autowired
	private XenonStager remoteToTargetStager;
	
	@Autowired
	private DeleteJobTask deleteJobTask;

	@PostConstruct
	public void initialize() {
		List<Job> submitted = repository.findAllByInternalState(JobState.STAGING_IN);
		for (Job job : submitted) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			// We re-request the job here because it may have changed while we were looping.
			job = repository.findById(job.getId()).get();
			jobLogger.info("Starting new job runner for job: " + job);
			try {
				// this used to be run in a seperate thread
				// so keeping the runnable part if we ever go back to that
				CwlStageInTask stageIn = new CwlStageInTask(job.getId(), sourceToRemoteStager, xenonService);
				stageIn.run();
			} catch (XenonException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
			}
		}
	
		// Restart stage out for jobs that were staging out
		List<Job> staging_out = repository.findAllByInternalState(JobState.STAGING_OUT);
		for (Job job : staging_out) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			// We re-request the job here because it may have changed while we were looping.
			job = repository.findById(job.getId()).get();
			try {
				// this used to be run in a seperate thread
				// so keeping the runnable part if we ever go back to that
				CwlStageOutTask stageOut = new CwlStageOutTask(job.getId(), (int) job.getAdditionalInfo().get("xenon.exitcode"), remoteToTargetStager, xenonService);
				stageOut.run();
			} catch (XenonException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
			}
		}
	}

	@Scheduled(fixedRateString = "${xenonflow.update.rate}", initialDelay=5000)
	public void update() {
		Scheduler scheduler;
		try {
			scheduler = xenonService.getScheduler();
		} catch (XenonException e) {
			logger.error("Error getting the xenon scheduler", e);
			return;
		}
		
		// The order here is important
		// update staging so staging is cancelled if
		// there is a cancel or deleter request.
		sourceToRemoteStager.updateStaging();
		remoteToTargetStager.updateStaging();
		
		// Cancel things that should no longer be running
		cancelWaitingJobs(scheduler);

		cancelRunningJobs(scheduler);
		
		deleteJobs();
		
		// Start calculating as soon as we can
		// otherwise we will be staging all tasks in first
		startReadyJobs();
		
		// Prioritize getting results
		startStageOutForFinishedJobs();
		
		// Finally we can start staging for new tasks
		startStageInForSubmittedJobs();
		
		// Update the rest
		updateWaitingJobs(scheduler);

		updateRunningJobs(scheduler);
	}

	private void updateRunningJobs(Scheduler scheduler) {
		List<Job> jobs = repository.findAllByInternalState(JobState.RUNNING);
		String[] jobIds = jobs.stream().map(j -> j.getXenonId()).toArray(String[]::new);
		try {
			JobStatus[] jobStatuses = scheduler.getJobStatuses(jobIds);
			for (int i=0; i<jobs.size(); i++) {
				Job job = jobs.get(i);
				JobStatus status = jobStatuses[i];
				Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
				
				if (status.hasException()) {
					if (status.getState().equals("INTERNAL_ERROR")) {
                        // We seem to have lost the connection to the scheduler -- we may retry later
                        jobLogger.error("Exception while retrieving status of job (scheduler connection lost?)", status.getException());
                        logger.error("Exception while retrieving status of job " + job.getName() + "(" + job.getId() + ") -- scheduler connection lost?",
                                status.getException());
                    } else if (status.getState().equals("UNKNOWN")) {
                        // We seem to have lost the job completely? -- try to recover the output
                        jobLogger.error("Could not find job ", status.getException());
                        logger.error("Could not find job " + job.getName() + "(" + job.getId() + ")", status.getException());
						tryRecoverJobOutput(job, status.getException());
					} else {
						// The job was cancelled or failed due to a timeout, node failure, preemption, etc. -- try to recover the output
                        jobLogger.error("Job failed or cancelled ", status.getException());
                        logger.error("Job failed or cancelled " + job.getName() + "(" + job.getId() + ")", status.getException());
                        tryRecoverJobOutput(job, status.getException());
					}
				} else if (status.isDone()) {
					jobService.setXenonState(job.getId(), status.getState());
					jobService.setXenonExitcode(job.getId(), status.getExitCode());
					try {
						if (status.getExitCode() != 0) {
							jobLogger.error("Job has finished with errors.");
							jobService.setJobState(job.getId(), JobState.RUNNING, JobState.FINISHED);
						} else {
							jobLogger.info("Jobs done.");
							jobService.setJobState(job.getId(), JobState.RUNNING, JobState.FINISHED);
						}
					} catch (StatePreconditionException e) {
						jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
						logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
					}
				}
			}
		} catch (XenonException e) {
			logger.error("Error during execution of update of running jobs", e);
		}
	}

	private void updateWaitingJobs(Scheduler scheduler) {
		List<Job> waiting = repository.findAllByInternalState(JobState.WAITING);
		String[] jobIds = waiting.stream().map(j -> j.getXenonId()).toArray(String[]::new);

		try {
			JobStatus[] jobStatuses = scheduler.getJobStatuses(jobIds);
			for (int i=0; i<waiting.size(); i++) {
				Job job = waiting.get(i);
				JobStatus status = jobStatuses[i];
				Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());

				// We re-request the job here because it may have changed while we were looping.
				job = repository.findById(job.getId()).get();
				String xenonJobId = job.getXenonId();
				
				if (status.hasException()) {
					if (status.getState().equals("INTERNAL_ERROR")) {
                        // We seem to have lost the connection to the scheduler -- we may retry later
                        jobLogger.error("Exception while retrieving status of job (scheduler connection lost?)", status.getException());
                        logger.error("Exception while retrieving status of job " + job.getName() + "(" + job.getId() + ") -- scheduler connection lost?",
                                status.getException());
                    } else if (status.getState().equals("UNKNOWN")) {
                        // We seem to have lost the job completely? -- try to recover the output
                        jobLogger.error("Could not find job ", status.getException());
                        logger.error("Could not find job " + job.getName() + "(" + job.getId() + ")", status.getException());
						tryRecoverJobOutput(job, status.getException());
					} else {
						// The job was cancelled or failed due to a timeout, node failure, preemption, etc. -- try to recover the output
                        jobLogger.error("Job failed or cancelled ", status.getException());
                        logger.error("Job failed or cancelled " + job.getName() + "(" + job.getId() + ")", status.getException());
                        tryRecoverJobOutput(job, status.getException());
					}
				} else if (xenonJobId != null && !xenonJobId.isEmpty()) {
					try {
						if (status.isRunning() && !status.hasException()) {
							jobService.setAdditionalInfo(job.getId(), "startedAt", new Date());
							jobService.setJobState(job.getId(), JobState.WAITING, JobState.RUNNING);
						} else if (status.isDone() && status.getExitCode() != 0) {
							jobLogger.error("Execution failed with code: " + status.getExitCode());
							jobService.setXenonExitcode(job.getId(), status.getExitCode());
							jobService.setErrorAndState(job.getId(), status.getException(), JobState.WAITING, JobState.FINISHED);
						} else if (status.isDone() && status.getExitCode() == 0) {
							jobLogger.info("Jobs done.");
							jobService.setXenonExitcode(job.getId(), status.getExitCode());
							jobService.setJobState(job.getId(), JobState.WAITING, JobState.FINISHED);
						}
						// If neither of the statements above holds then the job is probably pending.
						jobService.setXenonState(job.getId(), status.getState());
					} catch (StatePreconditionException e) {
						jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
						logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
					}
				}
			}
		} catch (XenonException e) {
			logger.error("Error during execution of update of running jobs", e);
		}
	}

	private void cancelRunningJobs(Scheduler scheduler) {
		List<Job> cancelRunning = repository.findAllByInternalState(JobState.RUNNING_CR);
		for (Job job : cancelRunning) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			// We re-request the job here because it may have changed while we were looping.
			job = repository.findById(job.getId()).get();
			try {
				String xenonJobId = job.getXenonId();
				if (xenonJobId != null && !xenonJobId.isEmpty()) {
					JobStatus status = scheduler.getJobStatus(xenonJobId);

					if (status.isRunning()) {
						status = scheduler.cancelJob(job.getXenonId());
						jobService.setXenonState(job.getId(), status.getState());
					} else {
						if (status.hasException() && !(status.getException() instanceof JobCanceledException)) {
							jobService.setXenonExitcode(job.getId(), status.getExitCode());
							jobService.setErrorAndState(job.getId(), status.getException(), JobState.RUNNING_CR, JobState.PERMANENT_FAILURE);
						} else {
							logger.debug("Cancelled job: " + job.getId() + " new status: " + status);
							jobService.setXenonState(job.getId(), status.getState());
							jobService.setXenonExitcode(job.getId(), status.getExitCode());
							jobService.setJobState(job.getId(), JobState.RUNNING_CR, JobState.CANCELLED);
						}
					}
				}
			} catch (NoSuchJobException e) {
				// It was running at some moment in time, so lets try to retrieve the output.
                tryRecoverJobOutput(job, e);
				if (job.getInternalState().isCancellationActive()) {
					// We can't find the job that we're cancelling. That's awesome let's continue!
					try {
						jobService.setJobState(job.getId(), job.getInternalState(), JobState.CANCELLED);
					} catch (StatePreconditionException e1) {
						jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
						logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
					}
				} else {
					jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
					logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				}
			} catch (XenonException | StatePreconditionException e) {
				jobLogger.error("Error while cancelling execution of " + job.getName() + "(" + job.getId() + ") -- scheduler connection lost?", e);
                logger.error("Error while cancelling execution of " + job.getName() + "(" + job.getId() + ") -- scheduler connection lost?", e);
			}
		}
	}

	private void cancelWaitingJobs(Scheduler scheduler) {
		List<Job> cancelWaiting = repository.findAllByInternalState(JobState.WAITING_CR);
		for (Job job : cancelWaiting) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			// We re-request the job here because it may have changed while we were looping.
			job = repository.findById(job.getId()).get();
			try {
				String xenonJobId = job.getXenonId();
				if (xenonJobId != null && !xenonJobId.isEmpty()) {
					JobStatus status = scheduler.getJobStatus(xenonJobId);

					if (!status.hasException()) {
						status = scheduler.cancelJob(job.getXenonId());
						logger.debug("Cancelled job: " + job.getId() + " new status: " + status);
					}

					jobService.setJobState(job.getId(), JobState.WAITING_CR, JobState.CANCELLED);
				}
			} catch (NoSuchJobException e) {
				tryRecoverJobOutput(job, e);
			} catch (XenonException | StatePreconditionException e) {
				jobLogger.error("Error while cancelling execution of " + job.getName() + "(" + job.getId() + ") -- scheduler connection lost?", e);
                logger.error("Error while cancelling execution of " + job.getName() + "(" + job.getId() + ") -- scheduler connection lost?", e);
			}
		}
	}
	
	private void deleteJobs() {
		List<Job> deleting = repository.findAllByInternalState(JobState.RUNNING_DELR);
		deleting.addAll(repository.findAllByInternalState(JobState.STAGING_IN_DELR));
		deleting.addAll(repository.findAllByInternalState(JobState.STAGING_OUT_DELR));
		deleting.addAll(repository.findAllByInternalState(JobState.WAITING_DELR));
		for (Job job : deleting) {
			deleteJobTask.deleteJob(job.getId());
		}
	}

	private void startStageOutForFinishedJobs() {
		List<Job> finished = repository.findAllByInternalState(JobState.FINISHED);
		for (Job job : finished) {

			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			// We re-request the job here because it may have changed while we were looping.
			job = repository.findById(job.getId()).get();
			try {
				job = jobService.setJobState(job.getId(), JobState.FINISHED, JobState.STAGING_OUT);
				CwlStageOutTask stageOut = new CwlStageOutTask(job.getId(), (int) job.getAdditionalInfo().get("xenon.exitcode"), remoteToTargetStager, xenonService);
				stageOut.run();
			} catch (XenonException | StatePreconditionException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
			}
		}
	}
	
	private void startStageInForSubmittedJobs() {
		List<Job> submitted = repository.findAllByInternalState(JobState.SUBMITTED);
		for (Job job : submitted) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			// We re-request the job here because it may have changed while we were looping.
			job = repository.findById(job.getId()).get();
			try {
				job = jobService.setJobState(job.getId(), JobState.SUBMITTED, JobState.STAGING_IN);
				CwlStageInTask stageIn = new CwlStageInTask(job.getId(), sourceToRemoteStager, xenonService);
				stageIn.run();
			} catch (XenonException | StatePreconditionException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
			}
		}
	}

	private void startReadyJobs() {
		List<Job> ready = repository.findAllByInternalState(JobState.STAGING_READY);
		for (Job job : ready) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			// We re-request the job here because it may have changed while we were looping.
			job = repository.findById(job.getId()).get();
			jobLogger.info("Starting new job runner for job: " + job.getId());
			try {
				jobService.setJobState(job.getId(), JobState.STAGING_READY, JobState.XENON_SUBMIT);
				CwlWorkflowTask submit = new CwlWorkflowTask(job.getId(), xenonService);
				submit.run();
			} catch (XenonException | StatePreconditionException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
			}
		}
	}
	
	private void tryRecoverJobOutput(Job job, Exception e) {
		logger.info("Could not recover job" + job + " it is probably lost...");
		Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
		// TODO: We should probably try harder here to figure out what went wrong
		// in additional info there may be a xenon.state.
		jobService.setErrorAndState(job.getId(), e, job.getInternalState(), JobState.SYSTEM_ERROR);
		try {
			// Let's try to stage back what we can
			CwlStageOutTask stageOut = new CwlStageOutTask(job.getId(), null, remoteToTargetStager, xenonService);
			stageOut.run();
		} catch (XenonException e1) {
			jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e1);
			logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e1);
		}
	}
}

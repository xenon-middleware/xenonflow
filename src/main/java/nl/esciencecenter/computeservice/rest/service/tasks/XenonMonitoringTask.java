package nl.esciencecenter.computeservice.rest.service.tasks;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.model.JobState;
import nl.esciencecenter.computeservice.rest.service.JobService;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.adaptors.schedulers.JobCanceledException;
import nl.esciencecenter.xenon.schedulers.JobStatus;
import nl.esciencecenter.xenon.schedulers.NoSuchJobException;
import nl.esciencecenter.xenon.schedulers.Scheduler;

public class XenonMonitoringTask implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(XenonMonitoringTask.class);

	private XenonService service;
	private JobRepository repository;
	private JobService jobService;

	public XenonMonitoringTask(XenonService service) {
		this.service = service;
		this.repository = service.getRepository();
		this.jobService = service.getJobService();
	}

	@Override
	public void run() {
		Scheduler scheduler;
		try {
			scheduler = service.getScheduler();
		} catch (XenonException e) {
			logger.error("Error getting the xenon scheduler", e);
			return;
		}

		cancelWatingJobs(scheduler);

		cancelRunningJobs(scheduler);

		startReadyJobs(scheduler);
		
		startStageOutForFinishedJobs(scheduler);

		updateWaitingJobs(scheduler);

		updateRunningJobs(scheduler);
	}

	private void updateRunningJobs(Scheduler scheduler) {
		List<Job> jobs = repository.findAllByInternalState(JobState.RUNNING);
		for (Job job : jobs) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			String xenonJobId = job.getXenonId();
			if (xenonJobId != null && !xenonJobId.isEmpty()) {
				try {
					JobStatus status = scheduler.getJobStatus(xenonJobId);
					if (status.isDone()) {
						jobService.setXenonState(job.getId(), status.getState());
						jobService.setXenonExitcode(job.getId(), status.getExitCode());

						if (status.hasException()) {
							jobLogger.error("Exception during execution", status.getException());
							jobLogger.error("Moving state from: " + job.getInternalState());
							jobService.setErrorAndState(job.getId(), status.getException(), JobState.RUNNING, JobState.PERMANENT_FAILURE);
						} else if (status.getExitCode() != 0) {
							jobLogger.error("Job has finished with errors.");
							jobService.setXenonExitcode(job.getId(), status.getExitCode());
							jobService.setJobState(job.getId(), JobState.RUNNING, JobState.FINISHED);
						} else {
							jobLogger.info("Jobs done.");
							jobService.setXenonExitcode(job.getId(), status.getExitCode());
							jobService.setJobState(job.getId(), JobState.RUNNING, JobState.FINISHED);
						}
					}
				} catch (NoSuchJobException e) {
					logger.info("Could not recover job" + job + " it is probably lost...");
					// TODO: We should probably try harder here to figure out what went wrong
					// in additional info there may be a xenon.state. Plus we could try
					// staging back the stdout and stderr of this job.
					jobService.setErrorAndState(job.getId(), e, job.getInternalState(), JobState.SYSTEM_ERROR);
				} catch (XenonException e) {
					jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
					logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				} catch (Exception e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void updateWaitingJobs(Scheduler scheduler) {
		List<Job> waiting = repository.findAllByInternalState(JobState.WAITING);
		for (Job job : waiting) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			String xenonJobId = job.getXenonId();
			if (xenonJobId != null && !xenonJobId.isEmpty()) {
				try {
					JobStatus status = scheduler.getJobStatus(xenonJobId);
	
					if (status.isRunning() && !status.hasException()) {
						jobService.setJobState(job.getId(), JobState.WAITING, JobState.RUNNING);
					} else if (status.hasException()) {
						jobLogger.error("Exception during execution", status.getException());
						jobService.setErrorAndState(job.getId(), status.getException(), JobState.WAITING, JobState.PERMANENT_FAILURE);
					} else if (status.isDone() && status.getExitCode() != 0) {
						jobLogger.error("Execution failed with code: " + status.getExitCode());
						jobService.setXenonExitcode(job.getId(), status.getExitCode());
						jobService.setErrorAndState(job.getId(), status.getException(), JobState.WAITING, JobState.FINISHED);
					} else if (status.isDone() && status.getExitCode() == 0) {
						jobLogger.info("Jobs done.");
						jobService.setXenonExitcode(job.getId(), status.getExitCode());
						jobService.setJobState(job.getId(), JobState.RUNNING, JobState.FINISHED);
					} else {
						jobLogger.error(
								"Exception during execution, Job is in an inconsistent state for workflowtask: " + job);
						jobService.setJobState(job.getId(), job.getInternalState(), JobState.PERMANENT_FAILURE);
					}
	
					jobService.setXenonState(job.getId(), status.getState());
				} catch (XenonException e) {
					jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
					logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void cancelRunningJobs(Scheduler scheduler) {
		List<Job> cancelRunning = repository.findAllByInternalState(JobState.RUNNING_CR);
		for (Job job : cancelRunning) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
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
			} catch (XenonException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void cancelWatingJobs(Scheduler scheduler) {
		List<Job> cancelWaiting = repository.findAllByInternalState(JobState.WAITING_CR);
		for (Job job : cancelWaiting) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
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
			} catch (XenonException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void startStageOutForFinishedJobs(Scheduler scheduler) {
		List<Job> finished = repository.findAllByInternalState(JobState.FINISHED);
		for (Job job : finished) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			try {
				service.getTaskScheduler().execute(new CwlStageOutTask(job.getId(), (int) job.getAdditionalInfo().get("xenon.exitcode"), service));
			} catch (XenonException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				;
			}
		}
	}

	private void startReadyJobs(Scheduler scheduler) {
		List<Job> ready = repository.findAllByInternalState(JobState.READY);
		for (Job job : ready) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			try {
				service.getTaskScheduler().execute(new CwlWorkflowTask(job.getId(), service));
			} catch (XenonException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
			}
		}
	}
}

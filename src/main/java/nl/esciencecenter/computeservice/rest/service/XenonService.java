package nl.esciencecenter.computeservice.rest.service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import nl.esciencecenter.computeservice.config.AdaptorConfig;
import nl.esciencecenter.computeservice.config.ComputeResource;
import nl.esciencecenter.computeservice.config.ComputeServiceConfig;
import nl.esciencecenter.computeservice.config.TargetAdaptorConfig;
import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobDescription;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.model.JobState;
import nl.esciencecenter.computeservice.rest.model.StatePreconditionException;
import nl.esciencecenter.computeservice.rest.service.tasks.CwlStageInTask;
import nl.esciencecenter.computeservice.rest.service.tasks.CwlStageOutTask;
import nl.esciencecenter.computeservice.rest.service.tasks.DeleteJobTask;
import nl.esciencecenter.computeservice.rest.service.tasks.XenonMonitoringTask;
import nl.esciencecenter.computeservice.utils.LoggingUtils;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.schedulers.Scheduler;

@Service
public class XenonService {
	private static final Logger logger = LoggerFactory.getLogger(XenonService.class);

	@Value("${xenon.config}")
	private String xenonConfigFile;

	@Value("${xenon.log.basepath}")
	private Path logBasePath;

	@Autowired
	private JobRepository repository;
	
	@Autowired
	private JobService jobService;
	
	private ThreadPoolTaskScheduler taskScheduler = null;
	private ThreadPoolTaskExecutor executor = null;
	private ComputeServiceConfig config = null;
	private Scheduler scheduler = null;
	private FileSystem remoteFileSystem = null;
	private FileSystem sourceFileSystem = null;
	private FileSystem targetFileSystem = null;

	public XenonService(ThreadPoolTaskScheduler taskScheduler, ThreadPoolTaskExecutor executor) throws IOException {
		this.taskScheduler = taskScheduler;
		this.executor = executor;
		// TODO: Watch the config file for changes?
		
		logger.debug("Got executor with pool size: " + this.executor.getCorePoolSize());
	}

	public void close() {
		try {
			if (scheduler != null && scheduler.isOpen()) {
				scheduler.close();
			}
			if (sourceFileSystem != null && sourceFileSystem.isOpen()) {
				sourceFileSystem.close();
			}
			if (remoteFileSystem != null && remoteFileSystem.isOpen()) {
				remoteFileSystem.close();
			}
		} catch (XenonException e) {
			logger.error("Error while shutting down xenon: ", e);
		}
		scheduler = null;
		sourceFileSystem = null;
		remoteFileSystem = null;
	}

	public void finalize() {
		close();
	}

	@PostConstruct
	private void initialize() throws XenonException, IOException {
		logger.debug("Loading xenon config from: " + xenonConfigFile);

		// Read xenon config
		setConfig(ComputeServiceConfig.loadFromFile(new File(xenonConfigFile)));
		// Sanity Check the config file.
		ComputeResource resource = getConfig().defaultComputeResource();

		// TODO: Is assertions the nicest way?
		assert(resource != null);
		assert(resource.getSchedulerConfig() != null);
		
		// Make sure everything is running.
		checkSchedulerStates();

		// Restart staging in for jobs that were staging in.
		List<Job> submitted = repository.findAllByInternalState(JobState.STAGING_IN);
		for (Job job : submitted) {
			Logger jobLogger = LoggerFactory.getLogger("jobs." + job.getId());
			// We re-request the job here because it may have changed while we were looping.
			job = repository.findOne(job.getId());
			jobLogger.info("Starting new job runner for job: " + job);
			try {
				executor.execute(new CwlStageInTask(job.getId(), this));
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
			job = repository.findOne(job.getId());
			try {
				executor.execute(new CwlStageOutTask(job.getId(), (int) job.getAdditionalInfo().get("xenon.exitcode"), this));
			} catch (XenonException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
			}
		}
		
		// Running, waiting and finished jobs should be automatically picked up by the
		// XenonWaitingTask
		taskScheduler.scheduleAtFixedRate(new XenonMonitoringTask(this), 1500);
	}

	public ThreadPoolTaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	public void setTaskScheduler(ThreadPoolTaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}
	
	public ThreadPoolTaskExecutor getExecutor() {
		return executor;
	}

	public void setExecutor(ThreadPoolTaskExecutor executor) {
		this.executor = executor;
	}

	public JobService getJobService() {
		return jobService;
	}

	public void setJobService(JobService jobService) {
		this.jobService = jobService;
	}
	
	private void checkSchedulerStates() throws XenonException {
		ComputeResource resource = getConfig().defaultComputeResource();
		AdaptorConfig schedulerConfig = resource.getSchedulerConfig();
		AdaptorConfig fileSystemConfig = resource.getFilesystemConfig();
		
		boolean useSchedulerFilesystem = fileSystemConfig == null;
		boolean recreateScheduler = false;
		boolean recreateFileSystem = false;
		if(useSchedulerFilesystem) {
			if (scheduler == null || !scheduler.isOpen() || remoteFileSystem == null || !remoteFileSystem.isOpen()) {
				recreateScheduler = true;
				recreateFileSystem = true;
			}
			
		} else {
			if (scheduler == null || !scheduler.isOpen()) {
				recreateScheduler = true;
			}
			if (remoteFileSystem == null || !remoteFileSystem.isOpen()) {
				recreateFileSystem = true;
			}
		}
		if (recreateScheduler) {
			logger.debug("Creating a scheduler to run jobs...");
			scheduler = Scheduler.create(schedulerConfig.getAdaptor(), schedulerConfig.getLocation(),
										 schedulerConfig.getCredential(), schedulerConfig.getProperties());
		}
		if (recreateFileSystem) {
			if (useSchedulerFilesystem && Scheduler.getAdaptorDescription(scheduler.getAdaptorName()).usesFileSystem()) {
				logger.debug("Using scheduler filesystem as a remote filesystem...");
				remoteFileSystem = scheduler.getFileSystem();
			} else {
				// Initialize remote filesystem
				logger.debug("Creating remote filesystem...");
				remoteFileSystem = FileSystem.create(fileSystemConfig.getAdaptor(), fileSystemConfig.getLocation(),
													 fileSystemConfig.getCredential(), fileSystemConfig.getProperties());
				logger.debug("Remote working directory: " + remoteFileSystem.getWorkingDirectory());
			}
		}
	}

	public Scheduler getScheduler() throws XenonException {
		checkSchedulerStates();
		return scheduler;
	}

	public Scheduler forceNewScheduler() throws XenonException {
		if (scheduler != null) {
			scheduler = null;
		}
		return getScheduler();
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public FileSystem getRemoteFileSystem() throws XenonException {
		checkSchedulerStates();
		return remoteFileSystem;
	}

	public void setRemoteFileSystem(FileSystem remoteFileSystem) {
		this.remoteFileSystem = remoteFileSystem;
	}

	public FileSystem getSourceFileSystem() throws XenonException {
		if (sourceFileSystem == null || !sourceFileSystem.isOpen()) {
			// Initialize local filesystem
			AdaptorConfig sourceConfig = getConfig().getSourceFilesystemConfig();
			logger.debug("Creating source filesystem..." + sourceConfig.getAdaptor() + " location: "
					+ sourceConfig.getLocation());
			logger.debug(sourceConfig.getAdaptor() + " " + sourceConfig.getLocation() + " " + sourceConfig.getCredential()
					+ " " + sourceConfig.getProperties());
			sourceFileSystem = FileSystem.create(sourceConfig.getAdaptor(), sourceConfig.getLocation(),
					sourceConfig.getCredential(), sourceConfig.getProperties());
		}
		return sourceFileSystem;
	}
	
	public FileSystem getTargetFileSystem() throws XenonException {
		if (targetFileSystem == null || !targetFileSystem.isOpen()) {
			// Initialize local filesystem
			TargetAdaptorConfig targetConfig = getConfig().getTargetFilesystemConfig();
			logger.debug("Creating target filesystem..." + targetConfig.getAdaptor() + " location: "
					+ targetConfig.getLocation());
			logger.debug(targetConfig.getAdaptor() + " " + targetConfig.getLocation() + " " + targetConfig.getCredential()
					+ " " + targetConfig.getProperties());
			targetFileSystem = FileSystem.create(targetConfig.getAdaptor(), targetConfig.getLocation(),
					targetConfig.getCredential(), targetConfig.getProperties());
		}
		return targetFileSystem;
	}

	public void setSourceFileSystem(FileSystem sourceFileSystem) {
		this.sourceFileSystem = sourceFileSystem;
	}

	public JobRepository getRepository() {
		return repository;
	}

	public void setRepository(JobRepository repository) {
		this.repository = repository;
	}

	public ComputeServiceConfig getConfig() {
		return config;
	}

	public void setConfig(ComputeServiceConfig config) {
		this.config = config;
	}

	public String getJobLogName(String name) {
		return logBasePath.resolve(name + ".log").toString();
	}

	public Job submitJob(JobDescription body) throws StatePreconditionException, XenonException {
		String uuid = UUID.randomUUID().toString();

		Logger jobLogger = LoggerFactory.getLogger("jobs." + uuid);
		LoggingUtils.addFileAppenderToLogger("jobs." + uuid, getJobLogName("jobs." + uuid));

		Job job = new Job();
		job.setId(uuid);
		job.setInput(body.getInput());
		job.setName(body.getName());
		job.setInternalState(JobState.SUBMITTED);
		job.setWorkflow(body.getWorkflow());

		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequestUri();
		builder.pathSegment(job.getId());
		job.setURI(builder.build().toString());

		builder.pathSegment("log");
		job.setLog(builder.build().toString());
		
		ServletUriComponentsBuilder b = ServletUriComponentsBuilder.fromCurrentRequestUri();
		b.replacePath("/");
		job.getAdditionalInfo().put("baseurl", b.build().toString());
		job.getAdditionalInfo().put("createdAt", new Date());

		job = repository.save(job);

		jobLogger.info("Submitted Job: " + job);
		
//		logger.info("Currently executing: " + executor.getActiveCount() + " threads.");
		
		//executor.execute(new CwlStageInTask(job.getId(), this));

		return job;
	}
	
	public Job cancelJob(String jobId) throws StatePreconditionException {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + jobId);
		
		jobLogger.info("Trying to cancel job " + jobId);
		
		Job job = repository.findOne(jobId);
		if (job != null && !job.getInternalState().isFinal()) {
			switch (job.getInternalState()) {
				case STAGING_IN:
					jobService.setJobState(jobId, JobState.STAGING_IN, JobState.STAGING_IN_CR);
					break;
				case WAITING:
					jobService.setJobState(jobId, JobState.WAITING, JobState.WAITING_CR);
					break;
				case RUNNING:
					jobService.setJobState(jobId, JobState.RUNNING, JobState.RUNNING_CR);
					break;
				case STAGING_OUT:
					jobService.setJobState(jobId, JobState.STAGING_OUT, JobState.STAGING_OUT_CR);
					break;
				default:
					if (!job.getInternalState().isFinal()) {
						jobService.setJobState(jobId, job.getInternalState(), JobState.CANCELLED);
					}
					break;
			}
		}
		
		return job;
	}

	public Job deleteJob(String jobId) throws XenonException, StatePreconditionException {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + jobId);
		
		jobLogger.info("Going to delete job " + jobId);
		
		Job job = repository.findOne(jobId);
		if (job != null) {
			if (!job.getInternalState().isFinal()) {
				job = cancelJob(jobId);
			}
			
			executor.execute(new DeleteJobTask(jobId, this));
		}
		
		return job;
	}
}

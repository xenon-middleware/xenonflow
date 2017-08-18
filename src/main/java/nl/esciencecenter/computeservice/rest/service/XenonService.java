package nl.esciencecenter.computeservice.rest.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import nl.esciencecenter.computeservice.config.AdaptorConfig;
import nl.esciencecenter.computeservice.config.ComputeResource;
import nl.esciencecenter.computeservice.config.ComputeServiceConfig;
import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.Job.StateEnum;
import nl.esciencecenter.computeservice.rest.model.JobDescription;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.service.tasks.CwlWorkflowTask;
import nl.esciencecenter.computeservice.rest.service.tasks.XenonMonitoringTask;
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
	JobRepository repository;

	private ThreadPoolTaskScheduler taskScheduler = null;

	private ComputeServiceConfig config = null;

	private Scheduler scheduler = null;
	private FileSystem remoteFileSystem = null;
	private FileSystem localFileSystem = null;

	public XenonService(ThreadPoolTaskScheduler taskScheduler) throws IOException {
		this.taskScheduler = taskScheduler;

		// TODO: Watch the config file for changes?
	}

	public void close() {
		try {
			if (scheduler != null && scheduler.isOpen()) {
				scheduler.close();
			}
			if (localFileSystem != null && localFileSystem.isOpen()) {
				localFileSystem.close();
			}
			if (remoteFileSystem != null && remoteFileSystem.isOpen()) {
				remoteFileSystem.close();
			}
		} catch (XenonException e) {
			logger.error("Error while shutting down xenon: ", e);
		}
		scheduler = null;
		localFileSystem = null;
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

		// Running and waiting jobs are automatically picked up by the
		// XenonWaitingTask
		taskScheduler.scheduleAtFixedRate(new XenonMonitoringTask(this), 1000);
	}

	public ThreadPoolTaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	public void setTaskScheduler(ThreadPoolTaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public Scheduler getScheduler() throws XenonException {
		if (scheduler == null) {
			// Initialize xenon scheduler
			ComputeResource resource = getConfig().defaultComputeResource();
			AdaptorConfig schedulerConfig = resource.getSchedulerConfig();
			Map<String, String> properties = new HashMap<>();

			properties.put("xenon.adaptors.slurm.ignore.version", "true");
			logger.debug("Creating a scheduler to run jobs...");
			scheduler = Scheduler.create(schedulerConfig.getAdaptor(), schedulerConfig.getLocation(),
					schedulerConfig.getCredential(), schedulerConfig.getProperties());
		}
		return scheduler;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public FileSystem getRemoteFileSystem() throws XenonException {
		if (remoteFileSystem == null || !remoteFileSystem.isOpen()) {
			// Initialize remote filesystem
			logger.debug("Creating remote filesystem...");
			ComputeResource resource = getConfig().defaultComputeResource();
			AdaptorConfig fileSystemConfig = resource.getFilesystemConfig();
			remoteFileSystem = FileSystem.create(fileSystemConfig.getAdaptor(), fileSystemConfig.getLocation(),
					fileSystemConfig.getCredential(), fileSystemConfig.getProperties());

			// TODO: Xenon should pick up on this automatically from the
			// location
			// in the release version it should. For now we hack it in here.
			remoteFileSystem.setWorkingDirectory(new Path("/var/scratch/bweel"));
		}
		return remoteFileSystem;
	}

	public void setRemoteFileSystem(FileSystem remoteFileSystem) {
		this.remoteFileSystem = remoteFileSystem;
	}

	public FileSystem getLocalFileSystem() throws XenonException {
		if (localFileSystem == null || !localFileSystem.isOpen()) {
			// Initialize local filesystem
			AdaptorConfig localConfig = getConfig().getLocalFilesystemConfig();
			logger.debug("Creating local filesystem..." + localConfig.getAdaptor() + " location: "
					+ localConfig.getLocation());
			logger.debug(localConfig.getAdaptor() + " " + localConfig.getLocation() + " " + localConfig.getCredential()
					+ " " + localConfig.getProperties());
			localFileSystem = FileSystem.create(localConfig.getAdaptor(), localConfig.getLocation(),
					localConfig.getCredential(), localConfig.getProperties());
		}
		return localFileSystem;
	}

	public void setLocalFileSystem(FileSystem localFileSystem) {
		this.localFileSystem = localFileSystem;
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

	/**
	 * Adding a specific file logger for a job. This is Logback specific!
	 * 
	 * Taken from:
	 * https://stackoverflow.com/questions/7824620/logback-set-log-file-name-programmatically
	 * 
	 * @param name
	 */
	private void addFileAppenderToLogger(String name) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
		fileAppender.setContext(loggerContext);
		fileAppender.setName(name);
		// set the file name
		fileAppender.setFile(getJobLogName(name));

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern("%d{yyyy-MMM-dd HH:mm:ss.SSS} %level - %msg%n");
		encoder.start();

		fileAppender.setEncoder(encoder);
		fileAppender.start();

		// attach the rolling file appender to the logger of your choice
		ch.qos.logback.classic.Logger logbackLogger = loggerContext.getLogger(name);
		logbackLogger.addAppender(fileAppender);
	}

	public Job submitJob(JobDescription body) throws XenonException {
		String uuid = UUID.randomUUID().toString();

		Logger jobLogger = LoggerFactory.getLogger("jobs." + uuid);
		addFileAppenderToLogger("jobs." + uuid);

		Job job = new Job();
		job.setId(uuid);
		job.setInput(body.getInput());
		job.setName(body.getName());
		job.setState(StateEnum.WAITING);
		job.setWorkflow(body.getWorkflow());

		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequestUri();
		builder.pathSegment(job.getId());
		job.setURI(builder.build().toString());

		builder.pathSegment("log");
		job.setLog(builder.build().toString());

		job = repository.save(job);

		jobLogger.info("Submitted Job: " + job);

		taskScheduler.execute(new CwlWorkflowTask(job.getId(), this));

		return job;
	}
}

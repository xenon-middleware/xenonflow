package nl.esciencecenter.computeservice.service;

import java.io.IOException;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nl.esciencecenter.computeservice.config.AdaptorConfig;
import nl.esciencecenter.computeservice.config.ComputeResource;
import nl.esciencecenter.computeservice.config.TargetAdaptorConfig;
import nl.esciencecenter.computeservice.config.XenonflowConfig;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.schedulers.Scheduler;

@Service
public class XenonService implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(XenonService.class);

	@Value("${xenonflow.config}")
	private String xenonConfigFile;

	@Value("${xenonflow.log.basepath}")
	private Path logBasePath;

	@Autowired
	private JobRepository repository;
	
	@Autowired
	private JobService jobService;
	
	private XenonflowConfig config = null;
	private Scheduler scheduler = null;
	private FileSystem remoteFileSystem = null;
	private FileSystem sourceFileSystem = null;
	private FileSystem targetFileSystem = null;
	private FileSystem cwlFileSystem = null;
	
	private String xenonflowHome = null;
	private String xenonflowFiles = null;

	public XenonService() throws IOException {
		// TODO: Watch the config file for changes?
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
			if (cwlFileSystem != null && cwlFileSystem.isOpen()) {
				cwlFileSystem.close();
			}
		} catch (XenonException e) {
			logger.error("Error while shutting down xenon: ", e);
		}
		scheduler = null;
		sourceFileSystem = null;
		remoteFileSystem = null;
		cwlFileSystem = null;
	}

	@PostConstruct
	public void initialize() throws XenonException, IOException {
		xenonflowHome = System.getenv("XENONFLOW_HOME");
		xenonflowFiles = System.getenv("XENONFLOW_FILES");

		if (xenonflowHome == null) {
			xenonflowHome = Paths.get(".").toAbsolutePath().normalize().toString();
		}
		logger.info("Xenonflow is using as home: " + xenonflowHome);

		// Read xenon config
		setConfig(XenonflowConfig.loadFromFile(xenonConfigFile, xenonflowHome, xenonflowFiles));
		// Sanity Check the config file.
		ComputeResource resource = getConfig().defaultComputeResource();

		// TODO: Is assertions the nicest way?
		assert(resource != null);
		assert(resource.getSchedulerConfig() != null);
		
		// Make sure everything is running.
		checkSchedulerStates();
	}

	public JobService getJobService() {
		return jobService;
	}

	public void setJobService(JobService jobService) {
		this.jobService = jobService;
	}
	
	public boolean checkSchedulerStates() throws XenonException {
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
			logger.info("Creating a scheduler to run jobs...");
			scheduler = Scheduler.create(schedulerConfig.getAdaptor(), schedulerConfig.getLocation(),
										 schedulerConfig.getCredential(), schedulerConfig.getProperties());
		} else {
	        logger.debug("Reusing existing scheduler");
	    }
		if (recreateFileSystem) {
			if (useSchedulerFilesystem && Scheduler.getAdaptorDescription(scheduler.getAdaptorName()).usesFileSystem()) {
				logger.info("Using scheduler filesystem as a remote filesystem...");
				remoteFileSystem = scheduler.getFileSystem();
			} else {
				// Initialize remote filesystem
				logger.info("Creating remote filesystem...");
				remoteFileSystem = FileSystem.create(fileSystemConfig.getAdaptor(), fileSystemConfig.getLocation(),
													 fileSystemConfig.getCredential(), fileSystemConfig.getProperties());
				logger.info("Remote working directory: " + remoteFileSystem.getWorkingDirectory());
			}
		}
		return recreateFileSystem || recreateScheduler;
	}

	public Scheduler getScheduler() throws XenonException {
		checkSchedulerStates();
		return scheduler;
	}
	
	public void closeScheduler() throws XenonException {
        if (scheduler != null) {
            if (scheduler.isOpen()) {
                scheduler.close();
            }
            scheduler = null;
        }
    }

	public Scheduler forceNewScheduler() throws XenonException {
		closeScheduler();
		return getScheduler();
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
			AdaptorConfig sourceConfig = getConfig().getSourceFilesystemConfig();
			logger.debug("Creating source filesystem..." +
					sourceConfig.getAdaptor() + " location: "
					+ sourceConfig.getLocation());

			logger.debug(sourceConfig.getAdaptor() + " " +
					sourceConfig.getLocation() + " " +
					sourceConfig.getCredential()
					+ " " + sourceConfig.getProperties());

			sourceFileSystem = FileSystem.create(
					sourceConfig.getAdaptor(),
					sourceConfig.getLocation(),
					sourceConfig.getCredential(),
					sourceConfig.getProperties());
		}
		return sourceFileSystem;
	}

	public FileSystem getTargetFileSystem() throws XenonException {
		if (targetFileSystem == null || !targetFileSystem.isOpen()) {
			TargetAdaptorConfig targetConfig = getConfig().getTargetFilesystemConfig();
			logger.debug("Creating target filesystem..."
					+ targetConfig.getAdaptor() + " location: "
					+ targetConfig.getLocation());

			logger.debug(targetConfig.getAdaptor() + " "
					+ targetConfig.getLocation() + " "
					+ targetConfig.getCredential()
					+ " " + targetConfig.getProperties());

			targetFileSystem = FileSystem.create(
					targetConfig.getAdaptor(),
					targetConfig.getLocation(),
					targetConfig.getCredential(),
					targetConfig.getProperties());
		}
		return targetFileSystem;
	}

	public void setSourceFileSystem(FileSystem sourceFileSystem) {
		this.sourceFileSystem = sourceFileSystem;
	}
	
	public FileSystem getCwlFileSystem() throws XenonException {
		if (cwlFileSystem == null || !cwlFileSystem.isOpen()) {
			AdaptorConfig cwlConfig = getConfig().getCwlFilesystemConfig();
			logger.debug("Creating cwl filesystem..." + cwlConfig.getAdaptor() + " location: "
					+ cwlConfig.getLocation());
			logger.debug(cwlConfig.getAdaptor() + " " + cwlConfig.getLocation() + " " + cwlConfig.getCredential()
					+ " " + cwlConfig.getProperties());
			cwlFileSystem = FileSystem.create(cwlConfig.getAdaptor(), cwlConfig.getLocation(),
					cwlConfig.getCredential(), cwlConfig.getProperties());
		}
		return cwlFileSystem;
	}

	public void setCwlFileSystem(FileSystem cwlFileSystem) {
		this.cwlFileSystem = cwlFileSystem;
	}

	public JobRepository getRepository() {
		return repository;
	}

	public void setRepository(JobRepository repository) {
		this.repository = repository;
	}

	public XenonflowConfig getConfig() {
		return config;
	}

	public void setConfig(XenonflowConfig config) {
		this.config = config;
	}

	public String getJobLogName(String name) {
		return logBasePath.resolve(name + ".log").toString();
	}

	public String getXenonflowHome() {
		return xenonflowHome;
	}
}

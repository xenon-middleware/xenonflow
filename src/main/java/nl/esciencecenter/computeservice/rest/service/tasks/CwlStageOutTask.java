package nl.esciencecenter.computeservice.rest.service.tasks;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.computeservice.rest.service.staging.FileStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.FileToMapStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.StagingManifest;
import nl.esciencecenter.computeservice.rest.service.staging.XenonStager;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.schedulers.JobStatus;

public class CwlStageOutTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(CwlStageOutTask.class);
	
	private String jobId;
	private XenonService service;
	private JobRepository repository;
	private JobStatus status;

	public CwlStageOutTask(String jobId, JobStatus status, XenonService service) throws XenonException {
		this.jobId = jobId;
		this.service = service;
		this.repository = service.getRepository();
		this.status = status;
	}
	
	@Override
	public void run() {
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
		Job job = repository.findOne(jobId);
		try {
			XenonStager stager = new XenonStager(repository, service.getLocalFileSystem(), service.getRemoteFileSystem());
	        // Staging back output
	        StagingManifest manifestBack = new StagingManifest(jobId, new Path("out/" + job.getId() + "/"));
	        
	        Path remoteDirectory = job.getSandboxDirectory();
	        Path outPath = remoteDirectory.resolve("stdout.txt");
	        Path errPath = remoteDirectory.resolve("stderr.txt");
	        Path localErrPath = new Path(errPath.getFileNameAsString());
	        
	        manifestBack.add(new FileToMapStagingObject(outPath, "stdout"));
	        manifestBack.add(new FileStagingObject(errPath, localErrPath));
	        
	        if (status.getExitCode() == 0) {
	        	// TODO: Add output from cwl run
	        }
	        job = stager.stageOut(manifestBack);
		} catch (XenonException e) {
			jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
			return;
		} catch (IOException e) {
			jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
			return;
		}
	}

}

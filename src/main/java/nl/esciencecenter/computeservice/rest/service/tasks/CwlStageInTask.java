package nl.esciencecenter.computeservice.rest.service.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.computeservice.rest.model.Job;
import nl.esciencecenter.computeservice.rest.model.JobRepository;
import nl.esciencecenter.computeservice.rest.service.XenonService;
import nl.esciencecenter.computeservice.rest.service.staging.FileStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.StagingManifest;
import nl.esciencecenter.computeservice.rest.service.staging.StringToFileStagingObject;
import nl.esciencecenter.computeservice.rest.service.staging.XenonStager;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.Path;

public class CwlStageInTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(CwlStageInTask.class);
	
	private String jobId;
	private XenonService service;
	private JobRepository repository;

	public CwlStageInTask(String jobId, XenonService service) throws XenonException {
		this.jobId = jobId;
		this.service = service;
		this.repository = service.getRepository();
	}
	
	@Override
	public void run(){
		Logger jobLogger = LoggerFactory.getLogger("jobs."+jobId);
		Job job = repository.findOne(jobId);
		try {
			XenonStager stager = new XenonStager(repository, service.getLocalFileSystem(), service.getRemoteFileSystem());

			// Staging files
			StagingManifest manifest = new StagingManifest(jobId, job.getSandboxDirectory());
				        
	        // Add the workflow to the staging manifest
	        Path localWorkflow = new Path(job.getWorkflow());
	        Path workflowBaseName = new Path (localWorkflow.getFileNameAsString());
	        manifest.add(new FileStagingObject(localWorkflow, workflowBaseName));
	        
	        Path remoteJobOrder = null;
	        if (job.hasInput()) {
	        	remoteJobOrder = new Path("job-order.json");
	        	manifest.add(new StringToFileStagingObject(job.getInput().toString(), remoteJobOrder));
	        }
	        
	        job = stager.stageIn(manifest);
	        
	        Path remoteDirectory = service.getRemoteFileSystem().getWorkingDirectory().resolve(manifest.getTargetDirectory());
	        job.getAdditionalInfo().put("xenon.remote.directory", remoteDirectory.toString());
	        
	        job = repository.save(job);
		} catch (XenonException e) {
			jobLogger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);
			logger.error("Error during execution of " + job.getName() + "(" +job.getId() +")", e);;
		}
	}
}

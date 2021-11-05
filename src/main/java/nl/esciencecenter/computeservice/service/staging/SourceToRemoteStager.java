package nl.esciencecenter.computeservice.service.staging;

import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.service.JobService;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;

public class SourceToRemoteStager extends XenonStager {

	public SourceToRemoteStager(JobService jobService, JobRepository repository, XenonService service) {
		super(jobService, repository, service);
	}

	@Override
	protected FileSystem getTargetFileSystem() throws XenonException {
		return xenonService.getRemoteFileSystem();
	}

	@Override
	protected FileSystem getSourceFileSystem() throws XenonException {
		return xenonService.getSourceFileSystem();
	}
	
	@Override
	protected FileSystem getCwlFileSystem() throws XenonException {
		return xenonService.getCwlFileSystem();
	}

}

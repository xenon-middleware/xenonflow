package nl.esciencecenter.computeservice.service.staging;

import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.service.JobService;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;

public class RemoteToTargetStager extends XenonStager {

	public RemoteToTargetStager(JobService jobService, JobRepository repository, XenonService service) {
		super(jobService, repository, service);
	}

	@Override
	protected FileSystem getTargetFileSystem() throws XenonException {
		return service.getTargetFileSystem();
	}

	@Override
	protected FileSystem getSourceFileSystem() throws XenonException {
		return service.getRemoteFileSystem();
	}

}

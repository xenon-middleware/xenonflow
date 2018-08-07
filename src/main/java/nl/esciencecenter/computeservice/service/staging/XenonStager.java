package nl.esciencecenter.computeservice.service.staging;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.commonwl.cwl.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import nl.esciencecenter.computeservice.model.Job;
import nl.esciencecenter.computeservice.model.JobRepository;
import nl.esciencecenter.computeservice.model.JobState;
import nl.esciencecenter.computeservice.model.StatePreconditionException;
import nl.esciencecenter.computeservice.model.WorkflowBinding;
import nl.esciencecenter.computeservice.service.JobService;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.computeservice.service.tasks.XenonMonitoringTask;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.CopyMode;
import nl.esciencecenter.xenon.filesystems.CopyStatus;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class XenonStager {
	private static final Logger logger = LoggerFactory.getLogger(XenonMonitoringTask.class);

	private FileSystem sourceFileSystem;
	private FileSystem targetFileSystem;
	private JobService jobService;
	private JobRepository repository;
	private XenonService service;
	private HashMap<String, StagingJob> copyMap;
	
	private static class StagingJob {
		private final StagingManifest manifest;
		private final List<String> copyIds;
		
		public StagingJob(StagingManifest manifest, List<String> copyIds) {
			super();
			this.manifest = manifest;
			this.copyIds = copyIds;
		}
	}
	
	private static final class StagingOutJob extends StagingJob {
		private final int exitcode;
		
		public StagingOutJob(int exitcode, StagingManifest manifest, List<String> copyIds) {
			super(manifest, copyIds);
			this.exitcode = exitcode;
		}
	}

	public XenonStager(JobService jobService, JobRepository repository, FileSystem sourceFileSystem, FileSystem targetFileSystem, XenonService service) {
		this.sourceFileSystem = sourceFileSystem;
		this.targetFileSystem = targetFileSystem;
		this.jobService = jobService;
		this.repository = repository;
		this.service = service;
		this.copyMap = new LinkedHashMap<String, StagingJob>();		
	}

	/**
	 * Stages everything defined in the StagingManifest
	 * 
	 * - FileStagingObject are used to stage files from the sourceFileSystem to
	 * the targetFileSystem - StringToFileStagingObject are used to stage a
	 * literal string to the targetFileSystem
	 * 
	 * @param manifest
	 * @param sourceFileSystem
	 * @param targetFileSystem
	 * @throws XenonException 
	 * @throws StatePreconditionException 
	 */
	public List<String> doStaging(StagingManifest manifest) throws XenonException, StatePreconditionException {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + manifest.getJobId());

		// Make sure the target directory exists
		Path targetDirectory = targetFileSystem.getWorkingDirectory().resolve(manifest.getTargetDirectory()).toAbsolutePath();
		if (!targetFileSystem.exists(targetDirectory)) {
			jobLogger.info("Creating directory: " + targetDirectory);
			targetFileSystem.createDirectories(targetDirectory);
		}
		
		Path sourceDirectory = sourceFileSystem.getWorkingDirectory().toAbsolutePath();

		List<String> stagingIds = new LinkedList<String>();
		// Copy all the files there
		for (StagingObject stageObject : manifest) {
			if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				if (!sourcePath.isAbsolute()) {
					sourcePath = sourceDirectory.resolve(object.getSourcePath()).toAbsolutePath();
				}
				Path targetPath = targetDirectory.resolve(object.getTargetPath());
				Path targetDir = targetPath.getParent();
				if (!targetFileSystem.exists(targetDir)) {
					targetFileSystem.createDirectories(targetDir);
				}
				
				jobLogger.info("Copying from " + sourcePath + " to " + targetPath);
				String copyId = sourceFileSystem.copy(sourcePath, targetFileSystem, targetPath, CopyMode.REPLACE,
						false);
				stageObject.setCopyId(copyId);
				stagingIds.add(copyId);
			} else if (stageObject instanceof StringToFileStagingObject) {
				StringToFileStagingObject object = (StringToFileStagingObject) stageObject;
				Path targetPath = targetDirectory.resolve(object.getTargetPath());
				Path targetDir = targetPath.getParent();

				if (!targetFileSystem.exists(targetDir)) {
					targetFileSystem.createDirectories(targetDir);
				}
				
				jobLogger.info("Writing string to: " + targetPath);
				String contents = object.getSourceString();
				
				jobLogger.debug("Input contents: " + contents);
				
				PrintWriter out = new PrintWriter(targetFileSystem.writeToFile(targetPath));
				out.write(contents);
				out.close();
				
				stageObject.setBytesCopied(contents.length());
			} else if (stageObject instanceof DirectoryStagingObject) {
				DirectoryStagingObject object = (DirectoryStagingObject) stageObject;
				Path sourcePath = object.getSourcePath();
				if (!sourcePath.isAbsolute()) {
					sourcePath = sourceDirectory.resolve(object.getSourcePath()).toAbsolutePath();
				}
				Path targetPath = targetDirectory.resolve(object.getTargetPath());
				Path targetDir = targetPath.getParent();

				if (!targetFileSystem.exists(targetDir)) {
					targetFileSystem.createDirectories(targetDir);
				}
				
				jobLogger.info("Copying from " + sourcePath + " to " + targetPath);
				String copyId = sourceFileSystem.copy(sourcePath, targetFileSystem, targetPath, CopyMode.REPLACE,
						true);
				stageObject.setCopyId(copyId);
				stagingIds.add(copyId);
			}
		}
		
		return stagingIds;
	}
	
	public void updateStaging() {
		for(Iterator<Map.Entry<String,StagingJob>> stagingEntries=copyMap.entrySet().iterator(); stagingEntries.hasNext();) {
			Map.Entry<String,StagingJob> entry = stagingEntries.next();
			String jobId = entry.getKey();
			StagingJob stagingJob = entry.getValue();
			
			StagingManifest manifest = stagingJob.manifest;
			List<String> copyIds = stagingJob.copyIds;
			Logger jobLogger = LoggerFactory.getLogger("jobs." + jobId);
			
			Optional<Job> j = repository.findById(jobId);		
			if (!j.isPresent()) {
				logger.error("Could not find job with id: " + jobId);
				jobLogger.error("Could not find job with id: " + jobId);
				return;
			}
			Job job = j.get();
			
			try {
				// Check if the job has been cancelled
				if (job.getInternalState().isCancellationActive() || job.getInternalState().isDeletionActive()) {
					for (String id: copyIds) {
						sourceFileSystem.cancel(id);
					}
					stagingEntries.remove();
					continue;
				}
				
				// Check the status of the copies of the job
				for (Iterator<String> iterator=copyIds.iterator(); iterator.hasNext();) {
					String id = iterator.next();
					CopyStatus s = sourceFileSystem.getStatus(id);
					
					if (s.hasException()) {
						jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", s.getException());
						logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", s.getException());
						jobService.setErrorAndState(job.getId(), s.getException(), job.getInternalState(), JobState.PERMANENT_FAILURE);
						copyMap.remove(jobId);
					} else if (s.isDone()) {
						StagingObject stageObject = manifest.getByCopyid(id);
						stageObject.setBytesCopied(s.bytesCopied());
						iterator.remove();
					}
				}
				
				// All statuses have been updated, if there are no more copyIds were done with staging
				if (copyIds.isEmpty()) {
					// No longer staging files for this job
					stagingEntries.remove();
					if (job.getInternalState() == JobState.STAGING_IN) {
						job = jobService.setJobState(jobId, JobState.STAGING_IN, JobState.STAGING_READY);
						jobLogger.info("StageIn complete.");
						logger.info(job.getId()+": staging in complete.");
					} else if (job.getInternalState() == JobState.STAGING_OUT){
						WorkflowBinding files = null;
						if (manifest.size() > 0) {
				    		files = postStageout(job, manifest);
				    		jobLogger.info("Fixed output: " + files.toIndentedString());
				    	} else {
				    		jobLogger.warn("There are no files to stage.");
				    	}
				    	
				        jobLogger.info("StageOut complete.");
				        logger.info(job.getId()+": staging out complete.");
				        
				        int exitcode = ((StagingOutJob)stagingJob).exitcode;

				        if (!job.getInternalState().isFinal()) {
				        	if (exitcode == 0) {
				        		job = jobService.completeJob(jobId, files, JobState.STAGING_OUT, JobState.SUCCESS);
				        		logger.info(job.getId()+": job completed successfully.");
				        	} else {
				        		job = jobService.completeJob(jobId, files, JobState.STAGING_OUT, JobState.PERMANENT_FAILURE);
				        		logger.info(job.getId()+": job completed with errors, exitcode: " + exitcode);
				        	}
				        }
					}
				}
			} catch (XenonException | StatePreconditionException | IOException e) {
				jobLogger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				logger.error("Error during execution of " + job.getName() + "(" + job.getId() + ")", e);
				jobService.setErrorAndState(job.getId(), e, job.getInternalState(), JobState.SYSTEM_ERROR);
				copyMap.remove(jobId);
			}
		}
	}
	
	/**
	 * Do the staging from local to remote
	 * 
	 * @param manifest
	 * @return 
	 * @throws StatePreconditionException 
	 * @throws XenonException 
	 */
	public boolean stageIn(StagingManifest manifest) throws XenonException, StatePreconditionException {	
		List<String> stagingIds = doStaging(manifest);
		
		StagingJob stagingJob = new StagingJob(manifest, stagingIds);
		copyMap.put(manifest.getJobId(), stagingJob);
		
		return true;
	}

	/**
	 * Do the staging from remote to local and update the job.
	 * 
	 * FileStagingObject is used to stage files from remote to local -
	 * FileToStringStagingObject is used to read a remote file into a string in
	 * the job output - FileToMapStagingObject is used to read a remote file
	 * into a json object in the job output
	 * 
	 * @param manifest
	 * @return 
	 * @throws StatePreconditionException 
	 * @throws XenonException 
	 * @throws IOException 
	 */
	public boolean stageOut(StagingManifest manifest, int exitcode) throws StatePreconditionException, IOException, XenonException {
		try {
			List<String> stagingIds = doStaging(manifest);
			StagingOutJob stagingJob = new StagingOutJob(exitcode, manifest, stagingIds);
			copyMap.put(manifest.getJobId(), stagingJob);
		} catch (XenonException e) {
			jobService.setErrorAndState(manifest.getJobId(), e, JobState.STAGING_OUT, JobState.PERMANENT_FAILURE);
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public WorkflowBinding postStageout(Job job, StagingManifest manifest) throws IOException, XenonException {
		Logger jobLogger = LoggerFactory.getLogger("jobs." + manifest.getJobId());
		WorkflowBinding binding = job.getOutput();
		
		jobLogger.debug("Starting postStageout");
		for (StagingObject stageObject : manifest) {
			if (stageObject instanceof FileStagingObject) {
				FileStagingObject object = (FileStagingObject) stageObject;
				Parameter parameter = object.getParameter();
				
				String outputTarget = null;
				Object outputObject = null;
				
				UriComponentsBuilder b;
				if (service.getConfig().getTargetFilesystemConfig().isHosted()) {
					b = UriComponentsBuilder.fromUriString(manifest.getBaseurl());
					b.pathSegment(service.getConfig().getTargetFilesystemConfig().getBaseurl());
					b.pathSegment(manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
				} else {
					b = UriComponentsBuilder.fromUriString(targetFileSystem.getLocation().toString());
					b.pathSegment(manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
				}
				
				if (parameter != null && binding.containsKey(parameter.getId())) {
					if (parameter.getType().equals("File[]")) {
						List<HashMap<String, Object>> values = (List<HashMap<String, Object>>) binding.get(parameter.getId());
						for (HashMap<String, Object> value : values) {
							value.put("path", manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
							value.put("location", b.build().toString());
						}
					
						outputTarget = parameter.getId();
						outputObject = values;
					} else {
						HashMap<String, Object> value = (HashMap<String, Object>) binding.get(parameter.getId());
						value.put("path", manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
						value.put("location", b.build().toString());
					
						outputTarget = parameter.getId();
						outputObject = value;
					}
				} else {
					HashMap<String, Object> value = new HashMap<String, Object>();
					value.put("location", b.build().toString());
					value.put("basename", object.getTargetPath().getFileNameAsString());
					value.put("path", manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
					value.put("class", "File");
					value.put("size", object.getBytesCopied());
				
					outputTarget = object.getTargetPath().getFileNameAsString();
					outputObject = value;
				}
				binding.put(outputTarget, outputObject);
			} else if (stageObject instanceof DirectoryStagingObject) {
				fixDirectoryStagingObject(job, manifest, stageObject, binding, jobLogger);
			}
//			} else if (stageObject instanceof FileToStringStagingObject) {
//				FileToStringStagingObject object = (FileToStringStagingObject) stageObject;
//				Path sourcePath = object.getSourcePath();
//				String contents = IOUtils.toString(sourceFileSystem.readFromFile(sourcePath), "UTF-8");
//				object.setBytesCopied(contents.length());
//				
//				jobLogger.debug("Read contents from " + sourcePath + ": " + contents);
//				outputTarget = object.getTargetString();
//				outputObject = contents;
//			} else if (stageObject instanceof FileToMapStagingObject) {
//				FileToMapStagingObject object = (FileToMapStagingObject) stageObject;
//				Path sourcePath = object.getSourcePath();
//				String contents = IOUtils.toString(sourceFileSystem.readFromFile(sourcePath), "UTF-8");
//				object.setBytesCopied(contents.length());
//				jobLogger.debug("Read contents from " + sourcePath + ": " + contents);
//
//				if (contents.length() > 0) {
//					ObjectMapper mapper = new ObjectMapper(new JsonFactory());
//					TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
//					};
//					HashMap<String, Object> value = mapper.readValue(new StringReader(contents), typeRef);
//
//					outputTarget = object.getTargetString();
//					outputObject = value;
//				}
//			}
		}
		return binding;
	}
	
	@SuppressWarnings("unchecked")
	public void fixDirectoryStagingObject(Job job, StagingManifest manifest,
			StagingObject stageObject, WorkflowBinding binding, Logger jobLogger) {
		DirectoryStagingObject object = (DirectoryStagingObject) stageObject;
		Parameter parameter = object.getParameter();
		
		UriComponentsBuilder b;
		if (service.getConfig().getTargetFilesystemConfig().isHosted()) {
			b = UriComponentsBuilder.fromUriString(manifest.getBaseurl());
			b.pathSegment(service.getConfig().getTargetFilesystemConfig().getBaseurl());
			b.pathSegment(manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
		} else {
			b = UriComponentsBuilder.fromUriString(targetFileSystem.getLocation().toString());
			b.pathSegment(manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
		}

		String  outputTarget = object.getTargetPath().getFileNameAsString();
		Object outputObject = null;
		
		if (parameter != null && binding.containsKey(parameter.getId())) {
			if (parameter.getType().equals("Directory[]")) {
				List<HashMap<String, Object>> values = (List<HashMap<String, Object>>) binding.get(parameter.getId());
				for (HashMap<String, Object> value : values) {
					Path dirPath = manifest.getTargetDirectory().resolve(object.getTargetPath());
					value.put("path", dirPath.toString());
					value.put("location", b.build().toString());
					
					if (value.containsKey("listing")) {
						fixListingRecursive(b, value, dirPath);
					}
				}
			
				outputTarget = parameter.getId();
				outputObject = values;
			} else {
				HashMap<String, Object> value = (HashMap<String, Object>) binding.get(parameter.getId());
				value.put("path", manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
				value.put("location", b.build().toString());
			
				outputTarget = parameter.getId();
				outputObject = value;
			}
		} else {
			HashMap<String, Object> value = new HashMap<String, Object>();
			value.put("location", b.build().toString());
			value.put("basename", object.getTargetPath().getFileNameAsString());
			value.put("path", manifest.getTargetDirectory().resolve(object.getTargetPath()).toString());
			value.put("class", "Directory");
			
			outputObject = value;
		}
		
		binding.put(outputTarget, outputObject);
	}

	@SuppressWarnings("unchecked")
	private void fixListingRecursive(UriComponentsBuilder b, HashMap<String, Object> value, Path dirPath) {
		List<HashMap<String, Object>> listing = (List<HashMap<String, Object>>)value.get("listing");
		for (HashMap<String, Object> dirItem : listing) {
			UriComponentsBuilder c = b.cloneBuilder();
			Path dirItemPath = new Path((String)dirItem.get("path")).getFileName();
			c.pathSegment(dirItemPath.getFileNameAsString());
			
			dirItem.put("path", dirPath.resolve(dirItemPath).toString());
			dirItem.put("location", c.build().toString());
			
			if (dirItem.containsKey("listing")) {
				fixListingRecursive(c, dirItem, dirItemPath);
			}
		}
	}

	public void setFileSystems(FileSystem sourceFileSystem, FileSystem targetFileSystem) {
		this.sourceFileSystem = sourceFileSystem;
		this.targetFileSystem = targetFileSystem;
	}
}

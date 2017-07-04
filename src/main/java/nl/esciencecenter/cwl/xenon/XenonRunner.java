package nl.esciencecenter.cwl.xenon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import nl.esciencecenter.cwl.Workflow;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.config.AdaptorConfig;
import nl.esciencecenter.xenon.config.ComputeResource;
import nl.esciencecenter.xenon.config.XenonConfig;
import nl.esciencecenter.xenon.filesystems.CopyHandle;
import nl.esciencecenter.xenon.filesystems.CopyMode;
import nl.esciencecenter.xenon.filesystems.CopyStatus;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.schedulers.JobDescription;
import nl.esciencecenter.xenon.schedulers.JobHandle;
import nl.esciencecenter.xenon.schedulers.JobStatus;
import nl.esciencecenter.xenon.schedulers.Scheduler;

public class XenonRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(XenonRunner.class);

    private static byte[] buffer = new byte[64*1024];

    private static long print(FileSystem files, Path path, long offset) throws XenonException {
        if (offset == -1L && !files.exists(path)) {
            return -1L;
        }
        try {
            InputStream in = files.readFromFile(path);
            if (offset == -1) {
                offset = 0;
            }
            if (offset > 0) {
                if (in.skip(offset) < offset) {
                    throw new IOException("unterminated skip...");
                }
            }
            long ret = in.read(buffer);
            while (ret != -1) {
                System.out.print(new String(Arrays.copyOf(buffer, (int)ret)));
                offset += ret;
                ret = in.read(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XenonException e) {
            LOGGER.warn("Cannot read remote file", e);
            // file does not yet exist or something else...
        }
        return offset;
    }

    public static void main(String[] args) {
        try {

            LOGGER.info("Starting the " + XenonRunner.class.getSimpleName() + " example.");

            // TODO: nice argument parsing
            assert args.length > 5;
            assert "--xenon-config".equals(args[0]);           
            assert "--xenon-compute-resource".equals(args[2]);
            
            String xenonConfigFilename = args[1];
            String xenonComputeResource = args[3];
            String workflowFilename = args[4];

            // TODO: make cache dir configurable?
            String cacheDir = "/tmp/xenon-cwl-runner/workflows/" + UUID.randomUUID().toString();
            LOGGER.info("Writing files to " + cacheDir);
            
            
            LOGGER.info("Reading xenon configuration from " + xenonConfigFilename);
            XenonConfig config = XenonConfig.loadFromFile(new File(xenonConfigFilename));
            ComputeResource resource = config.get(xenonComputeResource);
            AdaptorConfig fileSystemConfig = resource.getFilesystemConfig();
            AdaptorConfig schedulerConfig = resource.getSchedulerConfig();
            
            LOGGER.info("Reading workflow from " + workflowFilename);
            Workflow workflow = Workflow.fromFile(new File(workflowFilename));
            

            // Creating local and remote files and filesystems
            LOGGER.info("Setting up remote file system using " + fileSystemConfig.getAdaptor() + " adaptor and " + fileSystemConfig.getLocation() + " as location");
            FileSystem remoteFileSystem =  FileSystem.create(fileSystemConfig.getAdaptor(), fileSystemConfig.getLocation());
            Path cachePath = remoteFileSystem.getEntryPath().resolve(cacheDir); // Utils.resolveWithEntryPath(files, remoteFileSystem, cacheDir);
            remoteFileSystem.createDirectories(cachePath);

            // Copy workflow to machine
            // TODO: copy other files to machine
            FileSystem localFileSystem = FileSystem.create("file", "/");
            Path localWorkflow = localFileSystem.getEntryPath().resolve(workflowFilename);
            String workflowBaseName = localWorkflow.getFileNameAsString();
            Path remoteWorkflow = cachePath.resolve(workflowBaseName);
            LOGGER.info("Copying the localWorkflow from " + localWorkflow + " to " + remoteWorkflow);
            CopyHandle h = localFileSystem.copy(localWorkflow, remoteFileSystem, remoteWorkflow, CopyMode.REPLACE, false);
            CopyStatus s = localFileSystem.waitUntilDone(h, 1000);
            if(s.hasException()){
            	LOGGER.error("OH MY GOD IT'S BURNIGN!: " + s.getException().getMessage());
            	s.getException().printStackTrace();
            	System.exit(-1);
            }

            LOGGER.info("Creating a JobDescription for the job we want to run...");
            JobDescription description = new JobDescription();
            description.setExecutable("cwl-runner");
            // TODO: pass other arguments to cwl-runner
            ArrayList<String> cwlArguments = new ArrayList<String>();
            cwlArguments.add(cacheDir + "/" + workflowBaseName);
            cwlArguments.addAll(Arrays.asList(Arrays.copyOfRange(args, 5, args.length)));
            
            System.out.println(Arrays.toString(cwlArguments.toArray()));
            
            description.setArguments(cwlArguments.toArray(new String[cwlArguments.size()]));
            description.setStdout(cacheDir + "/stdout.txt");
            description.setStderr(cacheDir + "/stderr.txt");

            Map<String, String> properties = new HashMap<>();
            properties.put("xenon.adaptors.slurm.ignore.version", "true");
            LOGGER.debug("Creating a scheduler to run the job...");
            Scheduler scheduler = Scheduler.create(schedulerConfig.getAdaptor(), schedulerConfig.getLocation(), schedulerConfig.getCredential(), schedulerConfig.getProperties()); //jobs.newScheduler(location.getScheme(), location.getAuthority(), null, properties);

            LOGGER.debug("Submitting the job...");
            JobHandle job = scheduler.submitJob(description);

            scheduler.waitUntilRunning(job, 0);

            // Creating local and remote files and filesystems
            Path outPath = remoteFileSystem.getEntryPath().resolve(job.getJobDescription().getStdout()); // Utils.resolveWithEntryPath(files, remoteFileSystem, job.getJobDescription().getStdout());
            Path errPath = remoteFileSystem.getEntryPath().resolve(job.getJobDescription().getStderr());// Utils.resolveWithEntryPath(files, remoteFileSystem, job.getJobDescription().getStderr());

            // Reading in the standard error and standard out
            //
            long outIndex = -1L;
            long errIndex = -1L;

            JobStatus status;
            do {
                outIndex = print(remoteFileSystem, outPath, outIndex);
                errIndex = print(remoteFileSystem, errPath, errIndex);
                status = scheduler.waitUntilDone(job, 5000L);
            } while (!status.isDone());

            while (!remoteFileSystem.exists(outPath)) {
                Thread.sleep(1000L);
            }
            while (!remoteFileSystem.exists(errPath)) {
                Thread.sleep(1000L);
            }

            print(remoteFileSystem, outPath, outIndex);
            print(remoteFileSystem, errPath, errIndex);

            LOGGER.debug("Closing the scheduler to free up resources...");
            scheduler.close();
            remoteFileSystem.close();
            localFileSystem.close();

            LOGGER.info(XenonRunner.class.getSimpleName() + " completed.");

        } catch (XenonException e) {
            LOGGER.error(XenonRunner.class.getSimpleName() + " example failed: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}

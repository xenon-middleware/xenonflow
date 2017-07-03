package nl.esciencecenter.xenon.cwl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.credentials.DefaultCredential;
import nl.esciencecenter.xenon.filesystems.CopyMode;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.schedulers.JobDescription;
import nl.esciencecenter.xenon.schedulers.JobHandle;
import nl.esciencecenter.xenon.schedulers.JobStatus;
import nl.esciencecenter.xenon.schedulers.Scheduler;

public class CWLRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CWLRunner.class);

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

            LOGGER.info("Starting the " + CWLRunner.class.getSimpleName() + " example.");

            // TODO: nice argument parsing
            assert args.length > 2;
            assert "--xenon-scheduler".equals(args[0]);           
            assert "--xenon-location".equals(args[2]);
            
            String schedulertype = args[1];
            String location = args[3];

            String workflowFilename = args[4];

            // TODO: support local?
            String fileScheme = "sftp";

            // TODO: make cache dir configurable?
            String cacheDir = "xenon-cwl-runner/workflows/" + UUID.randomUUID().toString();

            LOGGER.info("Writing files to " + cacheDir);

            // Creating local and remote files and filesystems
            FileSystem remoteFileSystem =  FileSystem.create(fileScheme, location); // files.newFileSystem(fileScheme, location.getAuthority(), null, null);
            Path cachePath = remoteFileSystem.getEntryPath().resolve(cacheDir); // Utils.resolveWithEntryPath(files, remoteFileSystem, cacheDir);
            remoteFileSystem.createDirectories(cachePath);

            // Copy workflow to machine
            // TODO: copy other files to machine
            FileSystem localFileSystem = FileSystem.create("local");
            Path localWorkflow = localFileSystem.getEntryPath().resolve(workflowFilename);
            Path remoteWorkflow = cachePath.resolve(workflowFilename);
            LOGGER.debug("Copying the localWorkflow");
            localFileSystem.copy(localWorkflow, remoteFileSystem, remoteWorkflow, CopyMode.REPLACE, false);

            LOGGER.debug("Creating a JobDescription for the job we want to run...");
            JobDescription description = new JobDescription();
            description.setExecutable("cwl-runner");
            // TODO: pass other arguments to cwl-runner
            String[] cwlArguments = Arrays.copyOfRange(args, 2, args.length);
            cwlArguments[0] = cacheDir + "/" + workflowFilename;
            description.setArguments(cwlArguments);
            description.setStdout(cacheDir + "/stdout.txt");
            description.setStderr(cacheDir + "/stderr.txt");

            Map<String, String> properties = new HashMap<>();
            properties.put("xenon.adaptors.slurm.ignore.version", "true");
            LOGGER.debug("Creating a scheduler to run the job...");
            Scheduler scheduler = Scheduler.create(schedulertype, location, new DefaultCredential(), properties); //jobs.newScheduler(location.getScheme(), location.getAuthority(), null, properties);

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

            LOGGER.info(CWLRunner.class.getSimpleName() + " completed.");

        } catch (XenonException e) {
            LOGGER.error(CWLRunner.class.getSimpleName() + " example failed: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

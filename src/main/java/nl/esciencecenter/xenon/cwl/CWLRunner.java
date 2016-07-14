package nl.esciencecenter.xenon.cwl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.xenon.Xenon;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonFactory;
import nl.esciencecenter.xenon.files.FileSystem;
import nl.esciencecenter.xenon.files.Files;
import nl.esciencecenter.xenon.files.Path;
import nl.esciencecenter.xenon.files.RelativePath;
import nl.esciencecenter.xenon.jobs.*;
import nl.esciencecenter.xenon.util.Utils;

public class CWLRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CWLRunner.class);

    private static byte[] buffer = new byte[64*1024];

    private static long print(Files files, Path path, long offset) throws XenonException {
        if (offset == -1L && !files.exists(path)) {
            return -1L;
        }
        try {
            InputStream in = files.newInputStream(path);
            if (offset == -1) {
                offset = 0;
            }
            if (offset > 0) {
                if (in.skip(offset) < offset) {
                    throw new IOException("unterminated skip...");
                }
            }
            long ret = 0;
            while (ret != -1) {
                ret = in.read(buffer);
                if (ret > 0) {
                    System.out.print(new String(Arrays.copyOf(buffer, (int)ret)));
                    offset += ret;
                }
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
            assert "--xenon-host".equals(args[0]);

            LOGGER.debug("Convert the command line parameter to a URI...");
            URI location = new URI(args[1]);

            String workflowFilename = args[2];

            // TODO: support local?
            String fileScheme = "sftp";

            LOGGER.debug("Creating a new Xenon...");
            Xenon xenon = XenonFactory.newXenon(null);
            // TODO: make cache dir configurable?
            String cacheDir = "xenon-cwl-runner/workflows/" + UUID.randomUUID().toString();

            LOGGER.info("Writing files to " + cacheDir);

            // Creating local and remote files and filesystems
            Files files = xenon.files();
            FileSystem remoteFileSystem = files.newFileSystem(fileScheme, location.getAuthority(), null, null);
            Path cachePath = Utils.resolveWithEntryPath(files, remoteFileSystem, cacheDir);
            files.createDirectories(cachePath);

            // Copy workflow to machine
            // TODO: copy other files to machine
            FileSystem localFileSystem = files.newFileSystem("local", null, null, null);
            Path localWorkflow = files.newPath(localFileSystem, new RelativePath(Utils.getLocalCWD(files).getRelativePath(), new RelativePath(workflowFilename)));
            Path remoteWorkflow = Utils.resolveWithRoot(files, cachePath, workflowFilename);
            LOGGER.debug("Copying the localWorkflow");
            files.copy(localWorkflow, remoteWorkflow);

            LOGGER.debug("Creating a JobDescription for the job we want to run...");
            JobDescription description = new JobDescription();
            description.setExecutable("cwl-runner");
            // TODO: pass other arguments to cwl-runner
            String[] cwlArguments = Arrays.copyOfRange(args, 2, args.length);
            cwlArguments[0] = cacheDir + "/" + workflowFilename;
            description.setArguments(cwlArguments);
            description.setStdout(cacheDir + "/stdout.txt");
            description.setStderr(cacheDir + "/stderr.txt");

            LOGGER.debug("Retrieving the Jobs API...");
            Jobs jobs = xenon.jobs();

            Map<String, String> properties = new HashMap<>();
            properties.put("xenon.adaptors.slurm.ignore.version", "true");
            LOGGER.debug("Creating a scheduler to run the job...");
            Scheduler scheduler = jobs.newScheduler(location.getScheme(), location.getAuthority(), null, properties);

            LOGGER.debug("Submitting the job...");
            Job job = jobs.submitJob(scheduler, description);

            jobs.waitUntilRunning(job, 0);

            // Creating local and remote files and filesystems
            Path outPath = Utils.resolveWithEntryPath(files, remoteFileSystem, job.getJobDescription().getStdout());
            Path errPath = Utils.resolveWithEntryPath(files, remoteFileSystem, job.getJobDescription().getStderr());

            // Reading in the standard error and standard out
            //
            long outIndex = -1L;
            long errIndex = -1L;

            JobStatus status;
            do {
                outIndex = print(files, outPath, outIndex);
                errIndex = print(files, errPath, errIndex);
                status = jobs.waitUntilDone(job, 5000L);
            } while (!status.isDone());

            while (!files.exists(outPath)) {
                Thread.sleep(1000L);
            }
            while (!files.exists(errPath)) {
                Thread.sleep(1000L);
            }

            print(files, outPath, outIndex);
            print(files, errPath, errIndex);

            LOGGER.debug("Closing the scheduler to free up resources...");
            jobs.close(scheduler);
            files.close(remoteFileSystem);

            LOGGER.debug("Ending Xenon to release all resources...");
            XenonFactory.endXenon(xenon);

            LOGGER.info(CWLRunner.class.getSimpleName() + " completed.");

        } catch (URISyntaxException | XenonException e) {
            LOGGER.error(CWLRunner.class.getSimpleName() + " example failed: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

package nl.esciencecenter.computeservice.rest.api;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import nl.esciencecenter.computeservice.model.WorkflowListEntry;
import nl.esciencecenter.computeservice.service.XenonService;
import nl.esciencecenter.computeservice.utils.InetUtils;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.filesystems.PathAttributes;

@CrossOrigin
@Controller
public class WorkflowsApiController implements WorkflowsApi {
	private static final Logger logger = LoggerFactory.getLogger(WorkflowsApiController.class);
	private static final Logger requestLogger = LoggerFactory.getLogger("requests");
	
	@Autowired
	private XenonService xenonService;
	
	private static final String[] supportedExtensions = {"yml", "yaml", "cwl", "json"};

	@Override
	public ResponseEntity<List<WorkflowListEntry>> getWorkflows(HttpServletRequest request) {
		requestLogger.info("GET all workflows request received from: " + InetUtils.getClientIpAddr(request));
		try {
			FileSystem cwlFileSystem = xenonService.getCwlFileSystem();
			Path workingDirectory = cwlFileSystem.getWorkingDirectory();
			
			List<WorkflowListEntry> workflows = new LinkedList<WorkflowListEntry>();
			
			for (PathAttributes file : cwlFileSystem.list(workingDirectory, true)) {
				Path workflowPath = workingDirectory.relativize(file.getPath());
				String workflowFileName = workflowPath.getFileNameAsString();
				if (!file.isDirectory() && StringUtils.endsWithAny(workflowFileName, supportedExtensions)) {
					WorkflowListEntry entry = new WorkflowListEntry(workflowFileName, workflowPath.toString());
					workflows.add(entry);
				}
			}
			return new ResponseEntity<List<WorkflowListEntry>>(workflows, HttpStatus.OK);
		} catch (XenonException e) {
			logger.error("Error during job cancellation request:", e);
			return new ResponseEntity<List<WorkflowListEntry>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}

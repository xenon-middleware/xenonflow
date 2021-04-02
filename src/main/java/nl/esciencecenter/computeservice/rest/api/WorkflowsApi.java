package nl.esciencecenter.computeservice.rest.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import nl.esciencecenter.computeservice.model.WorkflowListEntry;

@Api(value = "workflows")
public interface WorkflowsApi {

	@ApiOperation(value = "Get workflow list", notes = "", response = WorkflowListEntry.class, responseContainer = "List", tags = {})
	@ApiResponses(value = { @ApiResponse(code = 200, message = "List of workflows available on the server", response = WorkflowListEntry.class),
			@ApiResponse(code = 404, message = "workflows not found", response = WorkflowListEntry.class) })
	@RequestMapping(value = "/workflows", produces = { "application/json" }, method = RequestMethod.GET)
	ResponseEntity<List<WorkflowListEntry>> getWorkflows(HttpServletRequest request);
}
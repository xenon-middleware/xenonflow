package nl.esciencecenter.computeservice.rest.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import nl.esciencecenter.computeservice.model.Status;

@Api(value = "status")
public interface StatusApi {

	@ApiOperation(value = "Get status", notes = "", response = Status.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Status of the server", response = Status.class),
			@ApiResponse(code = 404, message = "status not found", response = Status.class) })
	@RequestMapping(value = "/status", produces = { "application/json" }, method = RequestMethod.GET)
	ResponseEntity<Status> getStatus();

}

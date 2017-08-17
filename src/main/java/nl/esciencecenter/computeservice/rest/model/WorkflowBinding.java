package nl.esciencecenter.computeservice.rest.model;

import java.util.HashMap;
import java.util.Objects;
import java.io.Serializable;
import javax.validation.constraints.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.esciencecenter.computeservice.rest.service.staging.XenonStager;

/**
 * WorkflowBinding
 */
public class WorkflowBinding extends HashMap<String, Object> implements Serializable {
	private static final Logger logger = LoggerFactory.getLogger(WorkflowBinding.class);

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash();
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			logger.error("Error mapping WorkflowBinding to json: ", e);
		}
		return super.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
}

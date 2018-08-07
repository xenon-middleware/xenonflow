package nl.esciencecenter.computeservice.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.esciencecenter.computeservice.utils.JacksonUtils;

/**
 * WorkflowBinding
 */
public class WorkflowBinding extends HashMap<String, Object> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 290701851370536682L;
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
	public String toIndentedString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			logger.error("Error mapping WorkflowBinding to json: ", e);
		}
		return super.toString();
	}
	
	public static WorkflowBinding fromFile(File file) throws JsonMappingException, IOException {
		String extension = FilenameUtils.getExtension(file.getName());
		
		ObjectMapper mapper = JacksonUtils.getMapperForFileType(extension);
		
		WorkflowBinding wb = mapper.readValue(file, WorkflowBinding.class);
		
		return wb;
	}
}

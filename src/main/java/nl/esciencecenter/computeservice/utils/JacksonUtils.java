package nl.esciencecenter.computeservice.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class JacksonUtils {
	private static final Set<String> yamlTypes = new HashSet<String>(Arrays.asList(new String[] {"yml", "yaml", "cwl"}));
	private static final Set<String> jsonTypes = new HashSet<String>(Arrays.asList(new String[] {"json"}));
	
	public static ObjectMapper getMapperForFileType(String type) throws JsonParseException {
		if(yamlTypes.contains(type)) {
			return new ObjectMapper(new YAMLFactory());
		} else if(jsonTypes.contains(type)) {
			return new ObjectMapper(new JsonFactory());
		} else {
			throw new JsonParseException(null, "Could not find a mapper for file type: " + type);
		}
	}
}

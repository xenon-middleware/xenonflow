package nl.esciencecenter.xenon.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import nl.esciencecenter.xenon.credentials.Credential;

public class XenonConfig {
	private Map<String, ComputeResource> computeResources;
	private static final Set<String> yamlTypes = new HashSet<String>(Arrays.asList(new String[] {"yml", "yaml"}));
	private static final Set<String> jsonTypes = new HashSet<String>(Arrays.asList(new String[] {"json"}));
	
	private static ObjectMapper getMapper(String type) throws JsonParseException {
		if(yamlTypes.contains(type)) {
			return new ObjectMapper(new YAMLFactory());
		} else if(jsonTypes.contains(type)) {
			return new ObjectMapper(new JsonFactory());
		} else {
			throw new JsonParseException(null, "Could not find a mapper for file type: " + type);
		}
	}
	
	/**
	 * Loads a XenonConfig from a yaml or json file. File type is deteremined by the file extension.
	 * 
	 * @param configfile The file to load
	 * @return XenonConfig with the loaded configuration
	 * 
	 * @throws JsonParseException When the extension is not recognized or jackson has difficulty parsing the file
	 * @throws JsonMappingException When jackson has problems mapping the file to java objects
	 * @throws IOException When the file is not found
	 */
	public static XenonConfig loadFromFile(File configfile) throws JsonParseException, JsonMappingException, IOException {
		String extension = FilenameUtils.getExtension(configfile.getName());
		ObjectMapper mapper = getMapper(extension);
		SimpleModule module = new SimpleModule();
		
		module.addDeserializer(Credential.class, new CredentialDeserializer());
		mapper.registerModule(module);

		XenonConfig config = mapper.readValue(configfile, XenonConfig.class);
        
        return config;
	}
	
	/**
	 * Loads a XenonConfig from a string with a certain format type.
	 * 
	 * @param configstring The configuration string to load
	 * @return XenonConfig with the loaded configuration
	 * 
	 * @throws JsonParseException When the type is not recognized or jackson has difficulty parsing the file
	 * @throws JsonMappingException When jackson has problems mapping the file to java objects
	 * @throws IOException When the file is not found
	 */
	public static XenonConfig loadFromString(String configstring, String type) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		SimpleModule module = new SimpleModule();
		
		module.addDeserializer(Credential.class, new CredentialDeserializer());
		mapper.registerModule(module);
		
		XenonConfig config = mapper.readValue(new StringReader(configstring), XenonConfig.class);
        
        return config;
	}
	
	public XenonConfig(@JsonProperty("ComputeResources") Map<String, ComputeResource> computeResources) {
		super();
		this.computeResources = computeResources;
	}
	
	public Map<String, ComputeResource> getComputeResources() {
		return computeResources;
	}

	public void setComputeResources(Map<String, ComputeResource> computeResources) {
		this.computeResources = computeResources;
	}
	
	/*
	 * A bunch of delegate methods from Map
	 */
	
	public int size() {
		return computeResources.size();
	}

	public boolean isEmpty() {
		return computeResources.isEmpty();
	}

	public boolean containsKey(Object key) {
		return computeResources.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return computeResources.containsValue(value);
	}

	public ComputeResource get(Object key) {
		return computeResources.get(key);
	}

	public Set<String> keySet() {
		return computeResources.keySet();
	}

	public Collection<ComputeResource> values() {
		return computeResources.values();
	}

	public Set<Entry<String, ComputeResource>> entrySet() {
		return computeResources.entrySet();
	}

	@Override
	public int hashCode() {
		return computeResources.hashCode();
	}

	public ComputeResource getOrDefault(Object key, ComputeResource defaultValue) {
		return computeResources.getOrDefault(key, defaultValue);
	}
}

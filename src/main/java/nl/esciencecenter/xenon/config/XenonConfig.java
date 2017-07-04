package nl.esciencecenter.xenon.config;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import nl.esciencecenter.xenon.credentials.Credential;

public class XenonConfig {
	private Map<String, ComputeResource> computeResources;
	
	public static XenonConfig loadFromFile(File configfile) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		SimpleModule module = new SimpleModule();
		
		module.addDeserializer(Credential.class, new CredentialDeserializer());
		mapper.registerModule(module);

		XenonConfig config = mapper.readValue(configfile, XenonConfig.class);
        
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

	public int hashCode() {
		return computeResources.hashCode();
	}

	public ComputeResource getOrDefault(Object key, ComputeResource defaultValue) {
		return computeResources.getOrDefault(key, defaultValue);
	}
}

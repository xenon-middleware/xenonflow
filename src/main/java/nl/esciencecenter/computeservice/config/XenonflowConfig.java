/**
 * Copyright 2013 Netherlands eScience Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.esciencecenter.computeservice.config;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import nl.esciencecenter.computeservice.utils.JacksonUtils;
import nl.esciencecenter.xenon.credentials.Credential;

public class XenonflowConfig {
	private static final Logger logger = LoggerFactory.getLogger(XenonflowConfig.class);

	private Map<String, ComputeResource> computeResources;
	
	@JsonProperty(value="default", required=false)
	private String defaultComputeResourceName = null;
	
	@JsonProperty(value="sourceFileSystem", required=true)
	private AdaptorConfig sourceFileSystemConfig;
	
	@JsonProperty(value="targetFileSystem", required=true)
	private TargetAdaptorConfig targetFileSystemConfig;
	
	@JsonProperty(value="cwlFileSystem", required=true)
	private AdaptorConfig cwlFileSystemConfig;
	
	
	/**
	 * Loads a XenonConfig from a yaml or json file. File type is deteremined by the file extension.
	 * 
	 * @param configFile The file to load
	 * @return XenonConfig with the loaded configuration
	 * 
	 * @throws JsonParseException When the extension is not recognized or jackson has difficulty parsing the file
	 * @throws JsonMappingException When jackson has problems mapping the file to java objects
	 * @throws IOException When the file is not found
	 */
	public static XenonflowConfig loadFromFile(String configFile, String xenonflowHome, String xenonflowFiles) throws IOException {
		String extension = FilenameUtils.getExtension(configFile);
		ObjectMapper mapper;
		XenonflowConfig config = null;

		try {
			mapper = JacksonUtils.getMapperForFileType(extension);
			SimpleModule module = new SimpleModule();
		
			module.addDeserializer(Credential.class, new CredentialDeserializer());
			mapper.registerModule(module);

			Path xenonflowConfigPath = Paths.get(xenonflowHome, configFile);
			logger.info("Loading xenon config from: " + xenonflowConfigPath.toString());
			String contents = new String(Files.readAllBytes(xenonflowConfigPath));
			contents = contents.replaceAll("\\$\\{XENONFLOW_FILES\\}", xenonflowFiles);
			contents = contents.replaceAll("\\$\\{XENONFLOW_HOME\\}", xenonflowHome);
			contents = contents.replaceAll("\\$XENONFLOW_HOME", xenonflowHome);
			config = mapper.readValue(contents, XenonflowConfig.class);
        
			if (config.getDefaultComputeResourceName() == null) {
				// use the first key as the default if it's not set in the file
				config.setDefaultComputeResourceName(config.keySet().iterator().next());
			}
		} catch (JsonParseException e) {
			logger.error("Error parsing configuration file: " + configFile, e);
			throw e;
		} catch (JsonMappingException e) {
			logger.error("Error mapping configuration file: " + configFile, e);
			throw e;
		} catch (IOException e) {
			logger.error("Error opening configuration file: " + configFile, e);
			throw e;
		}
		
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
	public static XenonflowConfig loadFromString(String configstring, String type) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		SimpleModule module = new SimpleModule();
		
		module.addDeserializer(Credential.class, new CredentialDeserializer());
		mapper.registerModule(module);
		
		XenonflowConfig config = mapper.readValue(new StringReader(configstring), XenonflowConfig.class);
		
		if (config.getDefaultComputeResourceName() == null) {
			// use the first key as the default if it's not set in the file
			config.setDefaultComputeResourceName(config.keySet().iterator().next());
		}
        
        return config;
	}
	
	@JsonCreator
	public XenonflowConfig(@JsonProperty(value="ComputeResources", required=true) Map<String, ComputeResource> computeResources,
								@JsonProperty(value="sourceFileSystem", required=true) AdaptorConfig sourceFileSystemConfig,
								@JsonProperty(value="targetFileSystem", required=true) TargetAdaptorConfig targetFileSystemConfig,
								@JsonProperty(value="cwlFileSystem", required=true) AdaptorConfig cwlFileSystemConfig) {
		super();
		this.computeResources = computeResources;
		this.sourceFileSystemConfig = sourceFileSystemConfig;
		this.targetFileSystemConfig = targetFileSystemConfig;
		this.cwlFileSystemConfig = cwlFileSystemConfig;
		
	}
	
	public Map<String, ComputeResource> getComputeResources() {
		return computeResources;
	}

	public void setComputeResources(Map<String, ComputeResource> computeResources) {
		this.computeResources = computeResources;
	}
	
	public String getDefaultComputeResourceName() {
		return defaultComputeResourceName;
	}

	public void setDefaultComputeResourceName(String defaultComputeResourceName) {
		this.defaultComputeResourceName = defaultComputeResourceName;
	}
	
	public ComputeResource defaultComputeResource() {
		if (this.defaultComputeResourceName != null){
			return this.get(this.defaultComputeResourceName);
		} else {
			return null;
		}
	}
	
	public AdaptorConfig getSourceFilesystemConfig() {
		return sourceFileSystemConfig;
	}

	public void setSourceFilesystemConfig(AdaptorConfig sourceFileSystemConfig) {
		this.sourceFileSystemConfig = sourceFileSystemConfig;
	}
	
	public TargetAdaptorConfig getTargetFilesystemConfig() {
		return targetFileSystemConfig;
	}

	public void setTargetFilesystemConfig(TargetAdaptorConfig targetFileSystemConfig) {
		this.targetFileSystemConfig = targetFileSystemConfig;
	}
	
	public AdaptorConfig getCwlFilesystemConfig() {
		return cwlFileSystemConfig;
	}

	public void setCwlFilesystemConfig(AdaptorConfig cwlFileSystemConfig) {
		this.cwlFileSystemConfig = cwlFileSystemConfig;
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

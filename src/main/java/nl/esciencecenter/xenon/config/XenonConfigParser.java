package nl.esciencecenter.xenon.config;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import nl.esciencecenter.xenon.credentials.Credential;

public class XenonConfigParser {
	public static XenonConfig load(String configfile) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		SimpleModule module = new SimpleModule();
		
		module.addDeserializer(Credential.class, new CredentialDeserializer());
		mapper.registerModule(module);

		XenonConfig config = mapper.readValue(new File(configfile), XenonConfig.class);
        
        return config;
	}
}

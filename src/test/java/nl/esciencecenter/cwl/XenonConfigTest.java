package nl.esciencecenter.cwl;

import nl.esciencecenter.xenon.config.XenonConfigParser;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import nl.esciencecenter.xenon.config.XenonConfig;

public class XenonConfigTest {
	@Test
	public void testConfigLoad(){
		try {
			XenonConfig config = XenonConfigParser.load("src/test/resources/config/example_config.yml");
			System.out.println(config);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

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

import static org.junit.Assert.assertNotNull;

import java.nio.file.Paths;

import org.junit.Test;

public class ComputeServiceConfigTest {
	@Test
	public void testConfigLoad() throws Exception {
		String xenonflowHome = Paths.get(".").toAbsolutePath().normalize().toString();
		ComputeServiceConfig config = ComputeServiceConfig.loadFromFile("src/test/resources/config/example_config.yml", xenonflowHome);
		assertNotNull("Configuration should not be null", config);
		assertNotNull("ComputeResource das5 should exist", config.get("das5"));
		assertNotNull("Default resource should exist", config.defaultComputeResource());
		assertNotNull("Source filesystem should exist", config.getSourceFilesystemConfig());
		assertNotNull("Target filesystem should exist", config.getTargetFilesystemConfig());
	}
	
	@Test
	public void testConfigFromString() throws Exception {
		ComputeServiceConfig config = ComputeServiceConfig.loadFromString("{"
				+ "	\"ComputeResources\": {"
				+ "		\"das5\": {"
				+ "     \"cwlCommand\": \"cwltool\","
				+ "			\"scheduler\": {"
				+ "				\"adaptor\": \"local\","
				+ "				\"location\": \"local://berend\""
				+ "			},"
				+ "			\"filesystem\": {"
				+ "				\"adaptor\": \"file\","
				+ "				\"location\": \"/\""
				+ "			}"
				+ "		}"
				+ "	},"
				+ " \"sourceFileSystem\": {"
				+ "    \"adaptor\": \"webdav\","
				+ "    \"location\": \"http://localhost:8080/\""
				+ "  },"
				+ " \"targetFileSystem\": {"
				+ "    \"adaptor\": \"file\","
				+ "    \"location\": \"/tmp/results\""
				+ "  }"
				+ "}", "json");
		assertNotNull("Configuration should not be null", config);
		assertNotNull("ComputeResource das5 should exist", config.get("das5"));
		assertNotNull("Default resource should exist", config.defaultComputeResource());
		assertNotNull("Source filesystem should exist", config.getSourceFilesystemConfig());
		assertNotNull("Target filesystem should exist", config.getTargetFilesystemConfig());
		assert (config.get("das5").getSchedulerConfig().getAdaptor().equals("local")); 
	}
}

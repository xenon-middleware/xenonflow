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

import java.io.File;

import org.junit.Test;
import org.springframework.util.Assert;

public class ComputeServiceConfigTest {
	@Test
	public void testConfigLoad() throws Exception {
		ComputeServiceConfig config = ComputeServiceConfig.loadFromFile(new File("src/test/resources/config/example_config.yml"));
		Assert.notNull(config, "Configuration should not be null");
		Assert.notNull(config.get("das5"), "ComputeResource das5 should exist");
		Assert.notNull(config.defaultComputeResource(), "Default resource should exist");
		Assert.notNull(config.getLocalFilesystemConfig(), "Local filesystem should exist");
	}
	
	@Test
	public void testConfigFromString() throws Exception {
		ComputeServiceConfig config = ComputeServiceConfig.loadFromString("{"
				+ "	\"ComputeResources\": {"
				+ "		\"das5\": {"
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
				+ " \"localFileSystem\": {"
				+ "    \"adaptor\": \"webdav\","
				+ "    \"location\": \"http://localhost:8080/\""
				+ "  }"
				+ "}", "json");
		Assert.notNull(config, "Configuration should not be null");
		Assert.notNull(config.get("das5"), "ComputeResource das5 should exist");
		Assert.notNull(config.defaultComputeResource(), "Default resource should exist");
		Assert.notNull(config.getLocalFilesystemConfig(), "Local filesystem should exist");
		assert (config.get("das5").getSchedulerConfig().getAdaptor().equals("local")); 
	}
}

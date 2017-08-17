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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = false)
public class ComputeResource {
	@JsonProperty("scheduler")
	private AdaptorConfig schedulerConfig;
	@JsonProperty("filesystem")
	private AdaptorConfig filesystemConfig;
	
	public AdaptorConfig getSchedulerConfig() {
		return schedulerConfig;
	}

	public void setSchedulerConfig(AdaptorConfig schedulerConfig) {
		this.schedulerConfig = schedulerConfig;
	}

	public AdaptorConfig getFilesystemConfig() {
		return filesystemConfig;
	}

	public void setFilesystemConfig(AdaptorConfig filesystemConfig) {
		this.filesystemConfig = filesystemConfig;
	}
}

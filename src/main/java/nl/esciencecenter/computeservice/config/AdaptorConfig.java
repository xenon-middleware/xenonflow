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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.credentials.DefaultCredential;

public class AdaptorConfig {
	private String adaptor;
	private String location;
	private Map<String, String> properties;
	private Credential credential;
	
	public AdaptorConfig(@JsonProperty("credential") Credential credential) {
		if (credential == null){
			this.credential = new DefaultCredential();
		} else {
			this.credential = credential;
		}
	}
	
	public String getAdaptor() {
		return adaptor;
	}
	
	public void setAdaptor(String adaptor) {
		this.adaptor = adaptor;
	}
	
	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}
	
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	
	public Credential getCredential() {
		return credential;
	}
	
	public void setCredential(Credential credential) {
		this.credential = credential;
	}
}

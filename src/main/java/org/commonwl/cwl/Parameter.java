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
package org.commonwl.cwl;

import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Parameter extends HashMap<String, Object> {
	private static final long serialVersionUID = -4237293788994574555L;

	private String id;
	private String type;
	private String label;
	
	public Parameter(
			@JsonProperty("id") String id,
			@JsonProperty("type") String type,
			@JsonProperty("label") String label
			){
		this.id = id;
		this.type = type;
		this.label = label;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
}
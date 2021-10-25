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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * InputParameter holds information on CWL input parameters.
 * 
 * @version 1.0
 * @since 1.0
 */
public class InputParameter extends Parameter {
	private static final long serialVersionUID = -3096344382428378961L;

	/**
	 * Create an InputParameter
	 * 
	 * @param id
	 * 			the id of the parameter
	 * @param type
	 * 			the type of the parameter
	 */
	public InputParameter(
			@JsonProperty("id") String id,
			@JsonProperty("type") String type
			){
		super(id, type, false);
	}
	
	public InputParameter(
			@JsonProperty("id") String id,
			@JsonProperty("type") String type,
			@JsonProperty("optional") boolean optional){
		super(id, type, optional);
	}

	@Override
	public String toString() {
		return "InputParameter [id=" + getId() + ", type=" + getType() + ", optional=" + isOptional()  +"]";
	}
}

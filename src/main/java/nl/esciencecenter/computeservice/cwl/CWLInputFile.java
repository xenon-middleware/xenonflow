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
package nl.esciencecenter.computeservice.cwl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class CWLInputFile {
	@Id
	@GeneratedValue
	private Long id;
	
	@Column(name="location")
	private String location;

	@Column(name="filename")
	private String filename;

	public CWLInputFile(String location, String filename) {
		super();
		this.location = location;
		this.filename = filename;
	}

	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getLocation() {
		return location;
	}
	
	public void setFile(String location) {
		this.location = location;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public String toString() {
		return "CWLInputFile [id=" + id + ", location=" + location + ", filename=" + filename + "]";
	}
}

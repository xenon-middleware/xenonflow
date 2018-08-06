package org.commonwl.cwl;

import java.util.HashMap;

import org.commonwl.cwl.deserialization.FileDirectoryDeserializer;
import org.commonwl.cwl.utils.CWLUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class FileDirEntry {
	@JsonProperty("class")
	String entryClass;
    String basename;
	String nameroot;
    String nameext;
    String location; 
    String path;

	public String getEntryClass() {
		return entryClass;
	}

	public void setEntryClass(String entryClass) {
		this.entryClass = entryClass;
	}

	public String getBasename() {
		return basename;
	}
		
	public void setBasename(String basename) {
		this.basename = basename;
	}
    public String getNameroot() {
		return nameroot;
	}

	public void setNameroot(String nameroot) {
		this.nameroot = nameroot;
	}

	public String getNameext() {
		return nameext;
	}

	public void setNameext(String nameext) {
		this.nameext = nameext;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	public boolean hasPath() {
		return path != null && path.length() > 0;
	}

	public boolean hasLocation() {
		return location != null & location.length() > 0;
	}

	public boolean isLocalLocation() {
		return CWLUtils.isLocalPath(location);
	}

	public static FileDirEntry fromHashMap(HashMap<String, Object> entry) {
		final ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		
		module.addDeserializer(FileDirEntry.class, new FileDirectoryDeserializer());
		mapper.registerModule(module);

		return mapper.convertValue(entry, FileDirEntry.class);
	}
}

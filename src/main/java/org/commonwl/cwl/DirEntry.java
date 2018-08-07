package org.commonwl.cwl;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DirEntry extends FileDirEntry {
	@JsonProperty("listing")
    List<FileDirEntry> listing;
}

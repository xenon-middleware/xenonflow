package org.commonwl.cwl;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileEntry extends FileDirEntry {
	@JsonProperty("checksum")
    String checksum;
	@JsonProperty("content")
    String content;
	@JsonProperty("size")
    int size;
}

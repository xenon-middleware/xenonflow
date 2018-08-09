package nl.esciencecenter.computeservice.service.staging;

import org.commonwl.cwl.Parameter;

import nl.esciencecenter.xenon.filesystems.Path;

public class CommandScriptStagingObject extends StringToFileStagingObject {

	public CommandScriptStagingObject(String source, Path targetPath, Parameter parameter) {
		super(source, targetPath, parameter);
	}

}

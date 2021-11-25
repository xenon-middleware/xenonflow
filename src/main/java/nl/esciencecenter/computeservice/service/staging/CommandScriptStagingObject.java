package nl.esciencecenter.computeservice.service.staging;

import java.util.Set;

import org.slf4j.Logger;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.filesystems.PosixFilePermission;

public class CommandScriptStagingObject extends StringToFileStagingObject {

	public CommandScriptStagingObject(String source, Path targetPath) {
		super(source, targetPath, null);
	}

	public String stage(Logger jobLogger, FileSystem sourceFileSystem, FileSystem targetFileSystem,
			Path sourceDirectory, Path targetDirectory) throws XenonException {
		String id = super.stage(jobLogger, sourceFileSystem, targetFileSystem, sourceDirectory, targetDirectory);
		
		Path targetPath = targetDirectory.resolve(getTargetPath());

		Set<PosixFilePermission> permissions = targetFileSystem.getAttributes(targetPath).getPermissions();
		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		permissions.add(PosixFilePermission.GROUP_EXECUTE);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		targetFileSystem.setPosixFilePermissions(targetPath, permissions);
		
		return id;
	}
}

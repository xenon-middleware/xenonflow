package nl.esciencecenter.computeservice.service.staging;

import java.io.PrintWriter;

import org.commonwl.cwl.Parameter;
import org.slf4j.Logger;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class StringToFileStagingObject extends BaseStagingObject {
	protected String sourceString;
	protected Path targetPath;

	public StringToFileStagingObject(String source, Path targetPath, Parameter parameter) {
		super(parameter);
		this.sourceString = source;
		this.targetPath = targetPath;
	}

	public String getSourceString() {
		return sourceString;
	}

	public void setSourceString(String sourceString) {
		this.sourceString = sourceString;
	}

	public Path getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(Path targetPath) {
		this.targetPath = targetPath;
	}

	@Override
	public String toString() {
		return "StringToFileStagingObject ["
				+ "sourceString=" + sourceString
				+ ", targetPath=" + targetPath
				+ ", parameter=" + parameter
				+ "]";
	}
	
	public String stage(Logger jobLogger, FileSystem sourceFileSystem, FileSystem targetFileSystem,
			Path sourceDirectory, Path targetDirectory) throws XenonException {
		Path targetPath = targetDirectory.resolve(getTargetPath());
		Path targetDir = targetPath.getParent();

		if (!targetFileSystem.exists(targetDir)) {
			targetFileSystem.createDirectories(targetDir);
		}
		
		jobLogger.info("Writing string to: " + targetPath);
		String contents = getSourceString();
		
		jobLogger.debug("Input contents: " + contents);
		
		PrintWriter out = new PrintWriter(targetFileSystem.writeToFile(targetPath));
		out.write(contents);
		out.close();

		setBytesCopied(contents.length());
		
		return null;
	}
}
package nl.esciencecenter.computeservice.service.staging;

import org.commonwl.cwl.Parameter;

public abstract class BaseStagingObject implements StagingObject {

	protected long bytesCopied = 0;
	protected String copyId;
	protected Parameter parameter;

	@SuppressWarnings("unused")
	private BaseStagingObject() {
		
	}

	public BaseStagingObject(Parameter parameter) {
		this.parameter = parameter;
	}

	public void setBytesCopied(long bytes) {
		bytesCopied = bytes;
	}

	public long getBytesCopied() {
		return bytesCopied;
	}

	public void setCopyId(String copyId) {
		this.copyId = copyId;
	}

	public String getCopyId() {
		return copyId;
	}

	public void setParameter(Parameter parameter) {
		this.parameter = parameter;
	}

	public Parameter getParameter() {
		return parameter;
	}
}

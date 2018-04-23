package nl.esciencecenter.computeservice.rest.service.staging;

public class BaseStagingObject implements StagingObject {

	protected long bytesCopied = 0;
	protected String copyId;

	@Override
	public void setBytesCopied(long bytes) {
		bytesCopied = bytes;
	}

	@Override
	public long getBytesCopied() {
		return bytesCopied;
	}

	@Override
	public void setCopyId(String copyId) {
		this.copyId = copyId;
	}

	public String getCopyId() {
		return copyId;
	}

}

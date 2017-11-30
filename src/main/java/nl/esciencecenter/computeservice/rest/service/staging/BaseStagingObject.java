package nl.esciencecenter.computeservice.rest.service.staging;

public class BaseStagingObject implements StagingObject {

	protected long bytesCopied = 0;

	@Override
	public void setBytesCopied(long bytes) {
		bytesCopied = bytes;
	}

	@Override
	public long getBytesCopied() {
		return bytesCopied;
	}

}

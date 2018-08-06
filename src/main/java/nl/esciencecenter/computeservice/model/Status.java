package nl.esciencecenter.computeservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Status {
	@JsonProperty("waiting")
	private int waiting;
	
	@JsonProperty("running")
	private int running;
	
	@JsonProperty("successful")
	private int successful;
	
	@JsonProperty("errored")
	private int errored;

	public Status(int waiting, int running, int successful, int errored) {
		this.waiting = waiting;
		this.running = running;
		this.successful = successful;
		this.errored = errored;
	}

	public int getWaiting() {
		return waiting;
	}

	public void setWaiting(int waiting) {
		this.waiting = waiting;
	}

	public int getRunning() {
		return running;
	}

	public void setRunning(int running) {
		this.running = running;
	}

	public int getSuccessful() {
		return successful;
	}

	public void setSuccessful(int successful) {
		this.successful = successful;
	}

	public int getErrored() {
		return errored;
	}

	public void setErrored(int errored) {
		this.errored = errored;
	}
}

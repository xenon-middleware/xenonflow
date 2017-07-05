package nl.esciencecenter.xenon.cwl.rest.api;


public class ApiException extends Exception{
	private int code;
	public ApiException (int code, String msg) {
		super(msg);
		this.code = code;
	}
}

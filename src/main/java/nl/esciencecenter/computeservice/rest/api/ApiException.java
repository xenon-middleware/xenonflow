package nl.esciencecenter.computeservice.rest.api;


public class ApiException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6639169966853674674L;
	private int code;
	public ApiException (int code, String msg) {
		super(msg);
		this.code = code;
	}
}

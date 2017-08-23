package nl.esciencecenter.computeservice.rest.api;


public class NotFoundException extends ApiException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 559963627069122565L;
	private int code;
	
	public NotFoundException (int code, String msg) {
		super(code, msg);
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
	public void setCode(int code) {
		this.code = code;
	}
}

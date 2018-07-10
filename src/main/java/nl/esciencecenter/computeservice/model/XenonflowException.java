package nl.esciencecenter.computeservice.model;

public class XenonflowException extends Exception {

	private static final long serialVersionUID = 7561477622540055276L;

	public XenonflowException() {
		super();
	}

	public XenonflowException(String string) {
		super(string);
	}

	public XenonflowException(String message, Throwable cause) {
		super(message, cause);
	}

	public XenonflowException(Throwable cause) {
        super(cause);
    }

}

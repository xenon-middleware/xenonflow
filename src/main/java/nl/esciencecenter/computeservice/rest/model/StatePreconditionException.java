package nl.esciencecenter.computeservice.rest.model;

public class StatePreconditionException extends Exception {

	public StatePreconditionException() {
		super();
	}

	public StatePreconditionException(String string) {
		super(string);
	}

	public StatePreconditionException(String message, Throwable cause) {
		super(message, cause);
	}

	public StatePreconditionException(Throwable cause) {
        super(cause);
    }

	/**
	 * 
	 */
	private static final long serialVersionUID = 7561477622540055275L;

}

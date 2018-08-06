package nl.esciencecenter.computeservice.model;

public class StatePreconditionException extends Exception {
	private static final long serialVersionUID = 7561477622540055275L;

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
}

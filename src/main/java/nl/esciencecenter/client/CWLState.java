package nl.esciencecenter.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableList;

public enum CWLState {
	WAITING("Waiting"),

	RUNNING("Running"),

	SUCCESS("Success"),

	CANCELLED("Cancelled"),

	SYSTEM_ERROR("SystemError"),

	TEMPORARY_FAILURE("TemporaryFailure"),

	PERMANENT_FAILURE("PermanentFailure");

	private String value;
	private static final ImmutableList<CWLState> finalStates;
	private static final ImmutableList<CWLState> errorStates;

	static {
		finalStates = ImmutableList.of(CWLState.SUCCESS, CWLState.CANCELLED, CWLState.PERMANENT_FAILURE,
				CWLState.TEMPORARY_FAILURE, CWLState.SYSTEM_ERROR);
		
		errorStates = ImmutableList.of(CWLState.SYSTEM_ERROR, CWLState.TEMPORARY_FAILURE, CWLState.PERMANENT_FAILURE);
	}

	CWLState(String value) {
		this.value = value;
	}

	@Override
	@JsonValue
	public String toString() {
		return String.valueOf(value);
	}

	@JsonCreator
	public static CWLState fromValue(String text) {
		for (CWLState b : CWLState.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}

	/**
	 * Return whether the CWLState is a final state.
	 *
	 * @return bool: True if a job in this state will remain in this state
	 *         indefinitely.
	 */
	public boolean isFinal() {
		return finalStates.contains(this);
	}

	public boolean isWaiting() {
		return this == WAITING;
	}

	public boolean isErrorState() {
		return errorStates.contains(this);
	}
	
	public boolean isRunning() {
		return this == RUNNING;
	}
	
	public boolean isSuccess() {
		return this == SUCCESS;
	}
}

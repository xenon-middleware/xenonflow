package nl.esciencecenter.computeservice.rest.model;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableList;

public enum JobState {
	// Normal Execution
	SUBMITTED("Submitted"),

	STAGING_IN("Stage_In"),
	
	STAGING_READY("Staging_Ready"),
	
	XENON_SUBMIT("Xenon_Submit"),

	WAITING("Waiting"),

	RUNNING("Running"),

	FINISHED("Finished"),

	STAGING_OUT("Staging_Out"),

	SUCCESS("Success"),

	// Cancellation

	STAGING_IN_CR("Staging_In_Cr"),

	WAITING_CR("Waiting_Cr"),

	RUNNING_CR("Running_Cr"),

	STAGING_OUT_CR("Staging_Out_Cr"),

	CANCELLED("Cancelled"),

	// Errors

	SYSTEM_ERROR("System_Error"),

	TEMPORARY_FAILURE("Temporary_Failure"),

	PERMANENT_FAILURE("Permanent_Failure");

	private String value;
	private static final ImmutableList<JobState> finalStates;
	private static final ImmutableList<JobState> cancellationStates;
	private static final ImmutableList<JobState> remoteStates;
	private static final ImmutableList<JobState> errorStates;
	private static final HashMap<JobState, String> stateStringMap;

	static {
		finalStates = ImmutableList.of(JobState.SUCCESS, JobState.CANCELLED, JobState.PERMANENT_FAILURE,
				JobState.TEMPORARY_FAILURE, JobState.SYSTEM_ERROR);

		cancellationStates = ImmutableList.of(JobState.STAGING_IN_CR, JobState.WAITING_CR, JobState.RUNNING_CR,
				JobState.STAGING_OUT_CR);

		remoteStates = ImmutableList.of(JobState.WAITING, JobState.WAITING_CR, JobState.RUNNING, JobState.RUNNING_CR);
		
		errorStates = ImmutableList.of(JobState.SYSTEM_ERROR, JobState.TEMPORARY_FAILURE, JobState.PERMANENT_FAILURE);

		stateStringMap = new HashMap<JobState, String>();
		stateStringMap.put(JobState.SUBMITTED, "Waiting");
		stateStringMap.put(JobState.STAGING_IN, "Waiting");
		stateStringMap.put(JobState.STAGING_READY, "Waiting");
		stateStringMap.put(JobState.XENON_SUBMIT, "Waiting");
		stateStringMap.put(JobState.WAITING, "Waiting");
		stateStringMap.put(JobState.RUNNING, "Running");
		stateStringMap.put(JobState.FINISHED, "Running");
		stateStringMap.put(JobState.STAGING_OUT, "Running");
		stateStringMap.put(JobState.SUCCESS, "Success");

		stateStringMap.put(JobState.STAGING_IN_CR, "Waiting");
		stateStringMap.put(JobState.WAITING_CR, "Waiting");
		stateStringMap.put(JobState.RUNNING_CR, "Running");
		stateStringMap.put(JobState.STAGING_OUT_CR, "Running");
		stateStringMap.put(JobState.CANCELLED, "Cancelled");

		stateStringMap.put(JobState.SYSTEM_ERROR, "SystemError");
		stateStringMap.put(JobState.TEMPORARY_FAILURE, "TemporaryFailure");
		stateStringMap.put(JobState.PERMANENT_FAILURE, "PermanentFailure");
	}

	JobState(String value) {
		this.value = value;
	}

	@Override
	@JsonValue
	public String toString() {
		return String.valueOf(value);
	}

	@JsonCreator
	public static JobState fromValue(String text) {
		for (JobState b : JobState.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}

	/**
	 * Return whether the JobState is a final state.
	 *
	 * @return bool: True if a job in this state will remain in this state
	 *         indefinitely.
	 */
	public boolean isFinal() {
		return finalStates.contains(this);
	}

	/**
	 * Return whether the JobState indicates that the job has been marked for
	 * cancellation, but is not cancelled yet. These are the _CR states.
	 * 
	 * @return bool: True if a job in this state has been marked for
	 *         cancellation.
	 */
	public boolean isCancellationActive() {
		return cancellationStates.contains(this);
	}

	/**
	 * Return whether the state is one in which we expect the remote resource to
	 * do something to advance it to the next state. These are WAITING, RUNNING,
	 * and the corresponding _CR states.
	 * 
	 * @return bool: True iff this state is remote.
	 */
	public boolean isRemote() {
		return remoteStates.contains(this);
	}

	/**
	 * Return a string containing the CWL state corresponding to this state.
	 * 
	 * @return A string describing the argument as a CWL state.
	 */
	public String toCwlStateString() {
		return stateStringMap.get(this);
	}

	public boolean isErrorState() {
		return errorStates.contains(this);
	}
	
	public boolean isSuccess() {
		return this == SUCCESS;
	}

	public boolean isFinished() {
		return this == FINISHED;
	}
}

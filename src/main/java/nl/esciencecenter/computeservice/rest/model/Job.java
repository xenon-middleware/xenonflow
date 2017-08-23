package nl.esciencecenter.computeservice.rest.model;

import java.io.Serializable;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import nl.esciencecenter.xenon.filesystems.Path;

/**
 * Job
 */
@Entity
public class Job implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -651298299479062306L;

	@JsonProperty("id")
	@Id
	private String id = null;

	@JsonProperty("name")
	@Column(name = "name")
	private String name = null;

	@JsonProperty("workflow")
	@Column(name = "workflow")
	private String workflow = null;

	@JsonProperty("input")
	@Column(name = "input", columnDefinition = "CLOB", nullable = true)
	private WorkflowBinding input = null;

	@JsonProperty("additionalInfo")
	@Column(name = "additionalInfo", columnDefinition = "CLOB", nullable = true)
	private HashMap<String, Object> additionalInfo = null;
		
	/**
	 * Xenon Related Values. Not added to json, and for internal use.
	 */
	@JsonIgnore
	@Column(name = "xenonid", columnDefinition = "varchar(256)", nullable = true)
	private String xenonId = null;

	public String getXenonId() {
		return xenonId;
	}

	public void setXenonId(String xenonId) {
		this.xenonId = xenonId;
	}
	
	@JsonIgnore
	@Column(name = "internalState")
	private JobState internalState = null;
	
	@NotNull
	public JobState getInternalState() {
		return internalState;
	}

	public void setInternalState(JobState internalState) throws Exception {
		if (this.internalState == null) {
			this.internalState = internalState;
		}else {
			throw new Exception("Cannot set state when state is not null");
		}
	}

	@JsonProperty("output")
	@Column(name = "output", columnDefinition = "CLOB", nullable = true)
	private WorkflowBinding output = null;

	@JsonProperty("log")
	@Column(name = "log", columnDefinition = "varchar(2048)")
	private String log = null;

	@JsonProperty("uri")
	@Column(name = "uri", columnDefinition = "varchar(2048)", nullable = true)
	private String uri;

	public Job id(String id) {
		this.id = id;
		return this;
	}

	/**
	 * Get id
	 * 
	 * @return id
	 **/
	@ApiModelProperty(example = "afcd1554-9604-11e6-bd3f-080027e8b32a", required = true, value = "")
	@NotNull
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Job name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * user supplied (non unique) name for this job
	 * 
	 * @return name
	 **/
	@ApiModelProperty(example = "myjob1", required = true, value = "user supplied (non unique) name for this job")
	@NotNull
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean hasName() {
		return this.name != null && !this.name.isEmpty();
	}

	public Job workflow(String workflow) {
		this.workflow = workflow;
		return this;
	}

	/**
	 * location of the workflow
	 * 
	 * @return workflow
	 **/
	@ApiModelProperty(example = "https://github.com/common-workflow-language/common-workflow-language/raw/master/v1.0/v1.0/wc-tool.cwl", required = true, value = "location of the workflow")
	@NotNull
	public String getWorkflow() {
		return workflow;
	}

	public void setWorkflow(String workflow) {
		this.workflow = workflow;
	}

	public Job input(WorkflowBinding input) {
		this.input = input;
		return this;
	}

	/**
	 * Get input
	 * 
	 * @return input
	 **/
	@ApiModelProperty(required = true, value = "")
	public WorkflowBinding getInput() {
		return input;
	}

	public void setInput(WorkflowBinding input) {
		this.input = input;
	}
	
	public boolean hasInput() {
		return input != null && !input.isEmpty();
	}

	/**
	 * Get state
	 * 
	 * @return state
	 **/
	@ApiModelProperty(example = "Running", required = true, value = "")
	public String getState() {
		return internalState.toCwlStateString();
	}

	public Job output(WorkflowBinding output) {
		this.output = output;
		return this;
	}

	/**
	 * Get output
	 * 
	 * @return output
	 **/
	@ApiModelProperty(required = true, value = "")
	public WorkflowBinding getOutput() {
		if (output == null) {
			output = new WorkflowBinding();
		}
		return output;
	}

	public void setOutput(WorkflowBinding output) {
		this.output = output;
	}

	public Job log(String log) {
		this.log = log;
		return this;
	}

	/**
	 * Get log
	 * 
	 * @return log
	 **/
	@ApiModelProperty(example = "http://localhost:5000/jobs/afcd1554-9604-11e6-bd3f-080027e8b32a/log", required = true, value = "")
	@NotNull
	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	@ApiModelProperty(required = false, value = "")
	@Transient
	public HashMap<String, Object> getAdditionalInfo() {
		if (additionalInfo == null) {
			additionalInfo = new HashMap<String, Object>();
		}
		additionalInfo.put("internalState", internalState);
		return additionalInfo;
	}

	public void setAdditionalInfo(HashMap<String, Object> additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Job job = (Job) o;
		return Objects.equals(this.id, job.id) && Objects.equals(this.name, job.name)
				&& Objects.equals(this.workflow, job.workflow) && Objects.equals(this.input, job.input)
				&& Objects.equals(this.internalState, job.internalState) && Objects.equals(this.output, job.output)
				&& Objects.equals(this.log, job.log) && Objects.equals(this.additionalInfo, job.additionalInfo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, workflow, input, internalState, output, log);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class Job {\n");

		sb.append("    id: ").append(toIndentedString(id)).append("\n");
		sb.append("    name: ").append(toIndentedString(name)).append("\n");
		sb.append("    workflow: ").append(toIndentedString(workflow)).append("\n");
		sb.append("    input: ").append(toIndentedString(input)).append("\n");
		sb.append("    state: ").append(toIndentedString(internalState)).append("\n");
		sb.append("    output: ").append(toIndentedString(output)).append("\n");
		sb.append("    log: ").append(toIndentedString(log)).append("\n");
		sb.append("    additionalInfo:").append(toIndentedString(additionalInfo)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}

	public void setURI(String uri) {
		this.uri = uri;
	}

	public String getUri() {
		return uri;
	}

	@JsonIgnore
	public Path getSandboxDirectory() {
		String dirString = hasName() ? getName() + "-" + getId() : getId();
		String niceString = Normalizer.normalize(dirString.toLowerCase(), Form.NFD)
		        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
		        .replaceAll("[^\\p{Alnum}]+", "-");
		return new Path(niceString);
	}

	public void changeState(JobState from, JobState to) throws Exception {
		if (this.internalState != from) {
			throw new Exception("Previous state: " + this.internalState + " was expected to be: " + from);
		} else {
			this.internalState = to;
		}
	}
}

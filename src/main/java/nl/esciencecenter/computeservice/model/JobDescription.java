package nl.esciencecenter.computeservice.model;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

/**
 * JobDescription
 */
public class JobDescription implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = 3752504204045147455L;

	@JsonProperty("name")
	private String name = null;

	@JsonProperty("workflow")
	private String workflow = null;

	@JsonProperty("input")
	private WorkflowBinding input = null;

	public JobDescription name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * user supplied (non unique) name for this job
	 * 
	 * @return name
	 **/
	@ApiModelProperty(example = "myjob1", value = "user supplied (non unique) name for this job")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean hasName() {
		return name != null;
	}

	public JobDescription workflow(String workflow) {
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

	public JobDescription input(WorkflowBinding input) {
		this.input = input;
		return this;
	}

	/**
	 * Get input
	 * 
	 * @return input
	 **/
	@ApiModelProperty(value = "")
	public WorkflowBinding getInput() {
		return input;
	}

	public void setInput(WorkflowBinding input) {
		this.input = input;
	}

	public boolean hasInput() {
		return input != null;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		JobDescription jobDescription = (JobDescription) o;
		return Objects.equals(this.name, jobDescription.name) && Objects.equals(this.workflow, jobDescription.workflow)
				&& Objects.equals(this.input, jobDescription.input);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, workflow, input);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class JobDescription {\n");

		sb.append("    name: ").append(toIndentedString(name)).append("\n");
		sb.append("    workflow: ").append(toIndentedString(workflow)).append("\n");
		sb.append("    input: ").append(toIndentedString(input)).append("\n");
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
}

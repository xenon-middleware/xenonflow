package nl.esciencecenter.xenon.cwl.rest.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import nl.esciencecenter.xenon.cwl.rest.model.WorkflowBinding;
import java.io.Serializable;
import javax.validation.constraints.*;
/**
 * Job
 */

public class Job  implements Serializable {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("workflow")
  private String workflow = null;

  @JsonProperty("input")
  private WorkflowBinding input = null;

  /**
   * Gets or Sets state
   */
  public enum StateEnum {
    WAITING("Waiting"),
    
    RUNNING("Running"),
    
    SUCCESS("Success"),
    
    CANCELLED("Cancelled"),
    
    SYSTEMERROR("SystemError"),
    
    TEMPORARYFAILURE("TemporaryFailure"),
    
    PERMANENTFAILURE("PermanentFailure");

    private String value;

    StateEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StateEnum fromValue(String text) {
      for (StateEnum b : StateEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("state")
  private StateEnum state = null;

  @JsonProperty("output")
  private WorkflowBinding output = null;

  @JsonProperty("log")
  private String log = null;

  public Job id(String id) {
    this.id = id;
    return this;
  }

   /**
   * Get id
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

  public Job workflow(String workflow) {
    this.workflow = workflow;
    return this;
  }

   /**
   * location of the workflow
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
   * @return input
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull
  public WorkflowBinding getInput() {
    return input;
  }

  public void setInput(WorkflowBinding input) {
    this.input = input;
  }

  public Job state(StateEnum state) {
    this.state = state;
    return this;
  }

   /**
   * Get state
   * @return state
  **/
  @ApiModelProperty(example = "Running", required = true, value = "")
  @NotNull
  public StateEnum getState() {
    return state;
  }

  public void setState(StateEnum state) {
    this.state = state;
  }

  public Job output(WorkflowBinding output) {
    this.output = output;
    return this;
  }

   /**
   * Get output
   * @return output
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull
  public WorkflowBinding getOutput() {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Job job = (Job) o;
    return Objects.equals(this.id, job.id) &&
        Objects.equals(this.name, job.name) &&
        Objects.equals(this.workflow, job.workflow) &&
        Objects.equals(this.input, job.input) &&
        Objects.equals(this.state, job.state) &&
        Objects.equals(this.output, job.output) &&
        Objects.equals(this.log, job.log);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, workflow, input, state, output, log);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Job {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    workflow: ").append(toIndentedString(workflow)).append("\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    output: ").append(toIndentedString(output)).append("\n");
    sb.append("    log: ").append(toIndentedString(log)).append("\n");
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


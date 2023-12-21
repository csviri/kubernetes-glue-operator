package io.csviri.operator.workflow.customresource.operator;

import io.csviri.operator.workflow.customresource.workflow.WorkflowSpec;

public class WorkflowOperatorSpec extends WorkflowSpec {

  // todo cleanup workflows on delete flag

  // todo use apiVersion in parent
  private String group;
  private String version;
  private String kind;

  public String getGroup() {
    return group;
  }

  public WorkflowOperatorSpec setGroup(String group) {
    this.group = group;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public WorkflowOperatorSpec setVersion(String version) {
    this.version = version;
    return this;
  }

  public String getKind() {
    return kind;
  }

  public WorkflowOperatorSpec setKind(String kind) {
    this.kind = kind;
    return this;
  }
}

package io.csviri.operator.workflow.customresource.operator;

import java.util.Objects;

import io.csviri.operator.workflow.customresource.workflow.WorkflowSpec;

public class WorkflowOperatorSpec extends WorkflowSpec {

  // todo cleanup workflows on delete flag

  private Parent parent;

  public Parent getParent() {
    return parent;
  }

  public WorkflowOperatorSpec setParent(Parent parent) {
    this.parent = parent;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    WorkflowOperatorSpec that = (WorkflowOperatorSpec) o;
    return Objects.equals(parent, that.parent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), parent);
  }

}

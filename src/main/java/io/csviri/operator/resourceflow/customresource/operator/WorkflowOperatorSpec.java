package io.csviri.operator.resourceflow.customresource.operator;

import java.util.Objects;

import io.csviri.operator.resourceflow.customresource.workflow.WorkflowSpec;
import io.fabric8.generator.annotation.Required;

public class WorkflowOperatorSpec extends WorkflowSpec {

  @Required
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

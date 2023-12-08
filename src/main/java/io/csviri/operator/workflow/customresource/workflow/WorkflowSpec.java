package io.csviri.operator.workflow.customresource.workflow;

import java.util.List;
import java.util.Objects;

public class WorkflowSpec {

  private List<DependentResourceSpec> resources;

  public List<DependentResourceSpec> getResources() {
    return resources;
  }

  public void setResources(List<DependentResourceSpec> resources) {
    this.resources = resources;
  }

  @Override
  public String toString() {
    return "WorkflowSpec{" +
        "resources=" + resources +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    WorkflowSpec that = (WorkflowSpec) o;
    return Objects.equals(resources, that.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resources);
  }
}

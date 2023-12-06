package io.csviri.operator.workflow.customresource.workflow;

import java.util.List;

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
}

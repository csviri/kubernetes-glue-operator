package io.csviri.operator.workflow.customresource.workflow;

import java.util.List;

public class WorkflowSpec {

  private List<DependentResourceSpec> resources;

  private List<RelatedResourceSpec> relatedResources;

  public List<DependentResourceSpec> getResources() {
    return resources;
  }

  public void setResources(List<DependentResourceSpec> resources) {
    this.resources = resources;
  }

  public List<RelatedResourceSpec> getRelatedResources() {
    return relatedResources;
  }

  public WorkflowSpec setRelatedResources(List<RelatedResourceSpec> relatedResources) {
    this.relatedResources = relatedResources;
    return this;
  }

  @Override
  public String toString() {
    return "WorkflowSpec{" +
        "resources=" + resources +
        ", relatedResources=" + relatedResources +
        '}';
  }
}

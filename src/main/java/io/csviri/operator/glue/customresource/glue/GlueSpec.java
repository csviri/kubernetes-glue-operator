package io.csviri.operator.glue.customresource.glue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GlueSpec {

  private List<DependentResourceSpec> resources = new ArrayList<>();

  private List<RelatedResourceSpec> relatedResources = new ArrayList<>();

  public List<DependentResourceSpec> getResources() {
    return resources;
  }

  public void setResources(List<DependentResourceSpec> resources) {
    this.resources = resources;
  }

  public List<RelatedResourceSpec> getRelatedResources() {
    return relatedResources;
  }

  public GlueSpec setRelatedResources(List<RelatedResourceSpec> relatedResources) {
    this.relatedResources = relatedResources;
    return this;
  }

  @Override
  public String toString() {
    return "GlueSpec{" +
        "resources=" + resources +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GlueSpec that = (GlueSpec) o;
    return Objects.equals(resources, that.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resources);
  }
}

package io.csviri.operator.workflow.customresource.workflow;

import java.util.Objects;

public class RelatedResourceSpec {

  // name for referencing the resource from templates and conditions (not name from object metadata)
  private String name;

  private String apiVersion;
  private String kind;
  private String resourceName;

  public String getApiVersion() {
    return apiVersion;
  }

  public RelatedResourceSpec setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
    return this;
  }

  public String getKind() {
    return kind;
  }

  public RelatedResourceSpec setKind(String kind) {
    this.kind = kind;
    return this;
  }

  public String getName() {
    return name;
  }

  public RelatedResourceSpec setName(String name) {
    this.name = name;
    return this;
  }

  public String getResourceName() {
    return resourceName;
  }

  public RelatedResourceSpec setResourceName(String resourceName) {
    this.resourceName = resourceName;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    RelatedResourceSpec that = (RelatedResourceSpec) o;
    return Objects.equals(apiVersion, that.apiVersion) && Objects.equals(kind, that.kind)
        && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiVersion, kind, name);
  }
}

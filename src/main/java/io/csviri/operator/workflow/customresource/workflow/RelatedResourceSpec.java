package io.csviri.operator.workflow.customresource.workflow;

import java.util.Objects;

public class RelatedResourceSpec {

  private String apiVersion;
  private String kind;
  private String name;

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

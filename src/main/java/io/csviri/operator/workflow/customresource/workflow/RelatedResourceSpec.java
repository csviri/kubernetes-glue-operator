package io.csviri.operator.workflow.customresource.workflow;

public class RelatedResourceSpec {

  private String group;
  private String version;
  private String kind;

  private String name;
  private String namespace;

  public String getGroup() {
    return group;
  }

  public RelatedResourceSpec setGroup(String group) {
    this.group = group;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public RelatedResourceSpec setVersion(String version) {
    this.version = version;
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

  public String getNamespace() {
    return namespace;
  }

  public RelatedResourceSpec setNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }
}

package io.csviri.operator.glue.customresource.operator;

public class Parent {

  private String apiVersion;
  private String kind;
  private String labelSelector;

  public Parent() {}

  public Parent(String apiVersion, String kind) {
    this.apiVersion = apiVersion;
    this.kind = kind;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public Parent setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
    return this;
  }

  public String getKind() {
    return kind;
  }

  public Parent setKind(String kind) {
    this.kind = kind;
    return this;
  }

  public String getLabelSelector() {
    return labelSelector;
  }

  public void setLabelSelector(String labelSelector) {
    this.labelSelector = labelSelector;
  }
}

package io.csviri.operator.resourceglue.customresource.operator;

public class Parent {

  private String apiVersion;
  private String kind;

  public Parent(String apiVersion, String kind) {
    this.apiVersion = apiVersion;
    this.kind = kind;
  }

  public Parent() {}

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
}

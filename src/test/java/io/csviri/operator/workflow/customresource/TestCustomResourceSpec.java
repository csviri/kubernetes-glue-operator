package io.csviri.operator.workflow.customresource;


public class TestCustomResourceSpec {

  private String value;

  public String getValue() {
    return value;
  }

  public TestCustomResourceSpec setValue(String value) {
    this.value = value;
    return this;
  }
}

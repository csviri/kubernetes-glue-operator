package io.csviri.operator.workflow.customresource.workflow;

public class JavaScriptConditionSpec extends ConditionSpec {

  private String condition;

  public String getCondition() {
    return condition;
  }

  public JavaScriptConditionSpec setCondition(String condition) {
    this.condition = condition;
    return this;
  }
}

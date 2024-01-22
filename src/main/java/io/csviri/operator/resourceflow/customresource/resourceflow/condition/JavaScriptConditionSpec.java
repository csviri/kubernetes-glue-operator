package io.csviri.operator.resourceflow.customresource.resourceflow.condition;

public class JavaScriptConditionSpec extends ConditionSpec {

  private String script;

  public String getScript() {
    return script;
  }

  public JavaScriptConditionSpec setScript(String script) {
    this.script = script;
    return this;
  }
}

package io.csviri.operator.resourceglue.customresource.glue.condition;

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

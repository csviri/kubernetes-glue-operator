package io.csviri.operator.workflow.customresource.workflow.condition;

public class PodsReadyConditionSpec extends ConditionSpec {

  private final boolean negated;

  public PodsReadyConditionSpec(boolean negated) {
    this.negated = negated;
  }

  public PodsReadyConditionSpec() {
    this(false);
  }

  public boolean isNegated() {
    return negated;
  }
}

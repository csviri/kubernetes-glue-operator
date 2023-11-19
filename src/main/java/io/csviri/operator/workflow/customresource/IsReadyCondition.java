package io.csviri.operator.workflow.customresource;

public class IsReadyCondition extends Condition {

    // not ready;
    private boolean negated = false;

    public boolean isNegated() {
        return negated;
    }

    public IsReadyCondition setNegated(boolean negated) {
        this.negated = negated;
        return this;
    }
}

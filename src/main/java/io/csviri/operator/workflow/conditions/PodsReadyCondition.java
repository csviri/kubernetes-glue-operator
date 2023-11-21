package io.csviri.operator.workflow.conditions;

import io.csviri.operator.workflow.customresource.Workflow;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class PodsReadyCondition<R extends HasMetadata> implements Condition<R,Workflow> {

    private final Readiness readiness = Readiness.getInstance();

    private final boolean negated;

    public PodsReadyCondition(boolean negated) {
        this.negated = negated;
    }


    @Override
    public boolean isMet(DependentResource<R, Workflow> dependentResource, Workflow workflow, Context<Workflow> context)
    {
        var met = dependentResource.getSecondaryResource(workflow,context).map(readiness::isReady).orElse(false);
        return negated != met;
    }
}

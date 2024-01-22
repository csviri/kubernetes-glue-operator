package io.csviri.operator.resourceflow.conditions;

import io.csviri.operator.resourceflow.customresource.workflow.ResourceFlow;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class PodsReadyCondition<R extends HasMetadata> implements Condition<R, ResourceFlow> {

  private final Readiness readiness = Readiness.getInstance();

  private final boolean negated;

  public PodsReadyCondition(boolean negated) {
    this.negated = negated;
  }


  @Override
  public boolean isMet(DependentResource<R, ResourceFlow> dependentResource,
      ResourceFlow resourceFlow,
      Context<ResourceFlow> context) {
    var met = dependentResource.getSecondaryResource(resourceFlow, context).map(readiness::isReady)
        .orElse(false);
    return negated != met;
  }
}

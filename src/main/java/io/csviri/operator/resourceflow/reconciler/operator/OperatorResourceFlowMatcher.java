package io.csviri.operator.resourceflow.reconciler.operator;

import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlow;

import static io.csviri.operator.resourceflow.reconciler.operator.ResourceFlowOperatorReconciler.PARENT_RELATED_RESOURCE_NAME;

// todo unit test
public class OperatorResourceFlowMatcher {

  boolean matchResourceFlows(ResourceFlow actual, ResourceFlow desired) {
    if (!actual.getSpec().getResources()
        .equals(desired.getSpec().getResources())) {
      return false;
    }
    var actualWithoutParent = actual.getSpec().getRelatedResources().stream()
        .filter(r -> !r.getName().equals(PARENT_RELATED_RESOURCE_NAME)).toList();
    return desired.getSpec().getRelatedResources().equals(actualWithoutParent);
  }

}

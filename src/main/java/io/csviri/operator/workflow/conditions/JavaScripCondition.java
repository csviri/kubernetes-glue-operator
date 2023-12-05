package io.csviri.operator.workflow.conditions;

import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class JavaScripCondition<R extends HasMetadata> implements Condition<R, Workflow> {

  private String condition;

  @Override
  public boolean isMet(DependentResource<R, Workflow> dependentResource, Workflow workflow,
      Context<Workflow> context) {


    return false;
  }

}

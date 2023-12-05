package io.csviri.operator.workflow;

import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.GenericKubernetesDependentResource;

public class GenericDependentResource
    extends GenericKubernetesDependentResource<Workflow> implements GarbageCollected<Workflow>,
    Updater<GenericKubernetesResource, Workflow>,
    Creator<GenericKubernetesResource, Workflow> {

  private GenericKubernetesResource desired;

  public GenericDependentResource(GenericKubernetesResource desired) {
    super(new GroupVersionKind(desired.getApiVersion(), desired.getKind()));
    this.desired = desired;
  }

  @Override
  protected GenericKubernetesResource desired(Workflow primary,
      Context<Workflow> context) {

    // todo how do I know if a resource is namespaced or not => explicit flag / and check fabric8

    if (desired.getMetadata().getNamespace() == null) {
      desired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
    }
    return desired;
  }

  @Override
  public Result<GenericKubernetesResource> match(GenericKubernetesResource actualResource,
      Workflow primary, Context<Workflow> context) {
    return Result.nonComputed(false);
  }
}

package io.csviri.operator.workflow;

import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

public class GenericDependentResource<R extends HasMetadata>
    extends CRUDKubernetesDependentResource<R, Workflow> {

  private R desired;

  public GenericDependentResource(Class<R> rClass, R desired) {
    super(rClass);
    this.desired = desired;
  }

  @Override
  protected R desired(Workflow primary,
      Context<Workflow> context) {

    // todo set name if not set, same as primary?
    // todo should this be closed at all?
    // note that a new resource instance is created for every dependent
    if (desired instanceof Namespaced && desired.getMetadata().getNamespace() == null) {
      desired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
    }
    return desired;
  }
}

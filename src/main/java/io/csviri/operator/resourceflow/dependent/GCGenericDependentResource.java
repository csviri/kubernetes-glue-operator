package io.csviri.operator.resourceflow.dependent;

import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;

public class GCGenericDependentResource extends GenericDependentResource
    implements GarbageCollected<ResourceFlow> {

  public GCGenericDependentResource(GenericKubernetesResource desired, String name) {
    super(desired, name);
  }

  public GCGenericDependentResource(String desiredTemplate, String name) {
    super(desiredTemplate, name);
  }
}

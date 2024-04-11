package io.csviri.operator.glue.dependent;

import io.csviri.operator.glue.customresource.glue.Glue;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;

public class GCGenericDependentResource extends GenericDependentResource
    implements GarbageCollected<Glue> {

  public GCGenericDependentResource(GenericKubernetesResource desired, String name) {
    super(desired, name);
  }

  public GCGenericDependentResource(String desiredTemplate, String name) {
    super(desiredTemplate, name);
  }
}

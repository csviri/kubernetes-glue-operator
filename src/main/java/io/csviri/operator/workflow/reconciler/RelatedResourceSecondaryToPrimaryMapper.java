package io.csviri.operator.workflow.reconciler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;

public class RelatedResourceSecondaryToPrimaryMapper
    implements SecondaryToPrimaryMapper<GenericKubernetesResource> {

  private Map<ResourceID, Set<ResourceID>> idMap = new ConcurrentHashMap<>();

  @Override
  public Set<ResourceID> toPrimaryResourceIDs(GenericKubernetesResource resource) {
    return idMap.get(new ResourceID(resource.getMetadata().getName(),
        resource.getMetadata().getNamespace()));
  }

  public void addResourceIDMapping(ResourceID resourceID, ResourceID workFlowId) {
    idMap.merge(resourceID, Set.of(workFlowId), (s1, s2) -> {
      s1.addAll(s2);
      return s1;
    });
  }

  public void removeMappingFor(ResourceID workflowID) {
    idMap.values().forEach(s -> s.remove(workflowID));
  }
}

package io.csviri.operator.workflow.reconciler;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

public class RelatedResourceSecondaryToPrimaryMapper
    implements SecondaryToPrimaryMapper<GenericKubernetesResource> {

  private final Map<ResourceID, Set<ResourceID>> idMap = new ConcurrentHashMap<>();

  @Override
  public Set<ResourceID> toPrimaryResourceIDs(GenericKubernetesResource resource) {
    var res = Mappers.fromOwnerReferences(false).toPrimaryResourceIDs(resource);
    var idMapped = idMap.get(
        new ResourceID(resource.getMetadata().getName(), resource.getMetadata().getNamespace()));
    if (idMapped != null) {
      res.addAll(idMapped);
    }
    return res;
  }

  public void addResourceIDMapping(Collection<ResourceID> resourceIDs, ResourceID workFlowId) {
    resourceIDs.forEach(resourceID -> idMap.merge(resourceID, Set.of(workFlowId), (s1, s2) -> {
      s1.addAll(s2);
      return s1;
    }));
  }

  public void removeMappingFor(ResourceID workflowID) {
    idMap.values().forEach(s -> s.remove(workflowID));
  }
}

package io.csviri.operator.resourceflow.reconciler.flow;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

public class RelatedResourceSecondaryToPrimaryMapper
    implements SecondaryToPrimaryMapper<GenericKubernetesResource> {

  private final Map<ResourceID, Set<ResourceID>> secondaryToPrimaryMap = new ConcurrentHashMap<>();

  @Override
  public Set<ResourceID> toPrimaryResourceIDs(GenericKubernetesResource resource) {
    // based on if GC or non GC dependent it can have different mapping
    var res = Mappers.fromOwnerReferences(false).toPrimaryResourceIDs(resource);
    res.addAll(Mappers.fromDefaultAnnotations().toPrimaryResourceIDs(resource));
    // todo empty kind issue
    // var idMapped = secondaryToPrimaryMap.get(
    // new ResourceID(resource.getMetadata().getName(), resource.getMetadata().getNamespace()));
    // if (idMapped != null) {
    // res.addAll(idMapped);
    // }
    return res;
  }

  public void addResourceIDMapping(Collection<ResourceID> resourceIDs, ResourceID workFlowId) {
    Set<ResourceID> workflowIDSet = new HashSet<>();
    workflowIDSet.add(workFlowId);
    resourceIDs
        .forEach(resourceID -> secondaryToPrimaryMap.merge(resourceID, workflowIDSet, (s1, s2) -> {
          s1.addAll(s2);
          return s1;
        }));
  }

  public void removeMappingFor(ResourceID workflowID) {
    secondaryToPrimaryMap.entrySet().stream().forEach(e -> {
      e.getValue().remove(workflowID);
    });
    secondaryToPrimaryMap.entrySet().removeIf(e -> e.getValue().isEmpty());
  }
}

package io.csviri.operator.resourceflow.reconciler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.resourceflow.Utils;
import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

// todo test
class InformerRegister {

  private static final Logger log = LoggerFactory.getLogger(InformerRegister.class);

  private final Map<GroupVersionKind, Set<String>> gvkOfInformerToWorkflow = new HashMap<>();
  private final Map<String, Set<GroupVersionKind>> workflowToInformerGVK = new HashMap<>();
  private final Map<GroupVersionKind, RelatedResourceSecondaryToPrimaryMapper> relatedResourceMappers =
      new ConcurrentHashMap<>();

  public synchronized void deRegisterInformerOnResourceFlowChange(Context<ResourceFlow> context,
      ResourceFlow primary) {
    var registeredGVKSet =
        new HashSet<>(workflowToInformerGVK.get(primary.getMetadata().getName()));

    var currentGVKSet = primary.getSpec().getResources().stream()
        .map(Utils::getGVK)
        .collect(Collectors.toSet());

    primary.getSpec().getRelatedResources()
        .forEach(r -> currentGVKSet.add(new GroupVersionKind(r.getApiVersion(), r.getKind())));

    registeredGVKSet.removeAll(currentGVKSet);
    registeredGVKSet.forEach(gvk -> {
      log.debug("De-registering Informer on Workflow change for workflow: {} gvk: {}", primary,
          gvk);
      deRegisterInformer(gvk, primary, context);
    });
  }

  // todo tests + remake WO to use related resources
  public void registerInformerForRelatedResource(Context<ResourceFlow> context,
      ResourceFlow resourceFlow,
      GroupVersionKind gvk, String relatedResourceNamespace, List<String> relatedResourceNames) {
    registerInformer(context, gvk, resourceFlow);
    var mapper = relatedResourceMappers.get(gvk);

    mapper.addResourceIDMapping(
        relatedResourceNames.stream().map(n -> new ResourceID(n, relatedResourceNamespace))
            .collect(Collectors.toSet()),
        ResourceID.fromResource(resourceFlow));
  }

  @SuppressWarnings("unchecked")
  public InformerEventSource<GenericKubernetesResource, ResourceFlow> registerInformer(
      Context<ResourceFlow> context, GroupVersionKind gvk, ResourceFlow resourceFlow) {

    RelatedResourceSecondaryToPrimaryMapper mapper;
    synchronized (this) {
      relatedResourceMappers.putIfAbsent(gvk, new RelatedResourceSecondaryToPrimaryMapper());
      mapper = relatedResourceMappers.get(gvk);
      markEventSource(gvk, resourceFlow);
    }
    var newES = new InformerEventSource<>(InformerConfiguration.<GenericKubernetesResource>from(gvk)
        .withSecondaryToPrimaryMapper(mapper)
        .build(), context.eventSourceRetriever().eventSourceContextForDynamicRegistration());

    return (InformerEventSource<GenericKubernetesResource, ResourceFlow>) context
        .eventSourceRetriever()
        .dynamicallyRegisterEventSource(gvk.toString(), newES);

  }

  public synchronized void deRegisterInformer(GroupVersionKind groupVersionKind,
      ResourceFlow primary,
      Context<ResourceFlow> context) {
    var lastForGVK = unmarkEventSource(groupVersionKind, primary);
    if (lastForGVK) {
      log.debug("De-registering informer for gvk: {} primary: {}", groupVersionKind, primary);
      context.eventSourceRetriever().dynamicallyDeRegisterEventSource(groupVersionKind.toString());
    }
  }

  private synchronized void markEventSource(GroupVersionKind gvk,
      ResourceFlow resourceFlow) {

    gvkOfInformerToWorkflow.merge(gvk, new HashSet<>(Set.of(workflowId(resourceFlow))),
        (s1, s2) -> {
          s1.addAll(s2);
          return s1;
        });
    workflowToInformerGVK.merge(resourceFlow.getMetadata().getName(), new HashSet<>(Set.of(gvk)),
        (s1, s2) -> {
          s1.addAll(s2);
          return s1;
        });
  }

  private boolean unmarkEventSource(GroupVersionKind gvk,
      ResourceFlow resourceFlow) {

    var gvkSet = workflowToInformerGVK.get(resourceFlow.getMetadata().getName());
    gvkSet.remove(gvk);
    var es = gvkOfInformerToWorkflow.get(gvk);
    es.remove(workflowId(resourceFlow));
    return es.isEmpty();
  }

  private String workflowId(ResourceFlow resourceFlow) {
    return resourceFlow.getMetadata().getName() + "#" + resourceFlow.getMetadata().getNamespace();
  }

  private static <P extends HasMetadata> Optional<InformerEventSource<GenericKubernetesResource, P>> getInformerEventSource(
      Context<P> context, GroupVersionKind gvk) {
    try {
      return Optional
          .of((InformerEventSource<GenericKubernetesResource, P>) context.eventSourceRetriever()
              .getResourceEventSourceFor(GenericKubernetesResource.class, gvk.toString()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

}

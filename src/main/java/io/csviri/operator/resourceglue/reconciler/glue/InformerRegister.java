package io.csviri.operator.resourceglue.reconciler.glue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.resourceglue.Utils;
import io.csviri.operator.resourceglue.customresource.glue.Glue;
import io.csviri.operator.resourceglue.customresource.glue.RelatedResourceSpec;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

// todo unit test
class InformerRegister {

  private static final Logger log = LoggerFactory.getLogger(InformerRegister.class);

  private final Map<GroupVersionKind, Set<String>> gvkOfInformerToGlue = new HashMap<>();
  private final Map<String, Set<GroupVersionKind>> glueToInformerGVK = new HashMap<>();
  private final Map<GroupVersionKind, RelatedAndOwnedResourceSecondaryToPrimaryMapper> relatedResourceMappers =
      new ConcurrentHashMap<>();

  // todo test related resources deleting
  public synchronized void deRegisterInformerOnResourceFlowChange(Context<Glue> context,
      Glue primary) {
    var registeredGVKSet =
        new HashSet<>(glueToInformerGVK.get(primary.getMetadata().getName()));

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

  // todo tests
  public void registerInformerForRelatedResource(Context<Glue> context,
      Glue glue, RelatedResourceSpec relatedResourceSpec) {

    GroupVersionKind gvk =
        new GroupVersionKind(relatedResourceSpec.getApiVersion(), relatedResourceSpec.getKind());
    registerInformer(context, gvk, glue);

    var mapper = relatedResourceMappers.get(gvk);
    var relatedResourceNamespace =
        relatedResourceSpec.getNamespace() == null ? glue.getMetadata().getNamespace()
            : relatedResourceSpec.getNamespace();
    mapper.addResourceIDMapping(
        relatedResourceSpec.getResourceNames().stream()
            .map(n -> new ResourceID(n, relatedResourceNamespace))
            .collect(Collectors.toSet()),
        ResourceID.fromResource(glue));
  }

  @SuppressWarnings("unchecked")
  public InformerEventSource<GenericKubernetesResource, Glue> registerInformer(
      Context<Glue> context, GroupVersionKind gvk, Glue glue) {

    RelatedAndOwnedResourceSecondaryToPrimaryMapper mapper;
    synchronized (this) {
      relatedResourceMappers.putIfAbsent(gvk,
          new RelatedAndOwnedResourceSecondaryToPrimaryMapper());
      mapper = relatedResourceMappers.get(gvk);
      markEventSource(gvk, glue);
    }
    var newES = new InformerEventSource<>(InformerConfiguration.<GenericKubernetesResource>from(gvk)
        .withSecondaryToPrimaryMapper(mapper)
        .build(), context.eventSourceRetriever().eventSourceContextForDynamicRegistration());

    return (InformerEventSource<GenericKubernetesResource, Glue>) context
        .eventSourceRetriever()
        .dynamicallyRegisterEventSource(gvk.toString(), newES);

  }

  public synchronized void deRegisterInformer(GroupVersionKind groupVersionKind,
      Glue primary,
      Context<Glue> context) {
    var lastForGVK = unmarkEventSource(groupVersionKind, primary);
    if (lastForGVK) {
      var es = context.eventSourceRetriever()
          .dynamicallyDeRegisterEventSource(groupVersionKind.toString());
      es.ifPresent(i -> log.debug("De-registered informer for gvk: {} primary: {}",
          groupVersionKind, primary));
    }
  }

  public void deRegisterInformerForRelatedResources(Glue primary,
      Context<Glue> context) {
    cleanupRelatedResourceMappingForResourceFow(primary);

    primary.getSpec().getRelatedResources().forEach(r -> {
      var gvk = new GroupVersionKind(r.getApiVersion(), r.getKind());
      deRegisterInformer(gvk, primary, context);
    });
  }

  private void cleanupRelatedResourceMappingForResourceFow(Glue glue) {
    glue.getSpec().getRelatedResources().forEach(r -> {
      var gvk = new GroupVersionKind(r.getApiVersion(), r.getKind());
      relatedResourceMappers.get(gvk)
          .removeMappingFor(new ResourceID(glue.getMetadata().getName(),
              glue.getMetadata().getNamespace()));
    });
  }

  private synchronized void markEventSource(GroupVersionKind gvk,
      Glue glue) {

    gvkOfInformerToGlue.merge(gvk, new HashSet<>(Set.of(workflowId(glue))),
        (s1, s2) -> {
          s1.addAll(s2);
          return s1;
        });
    glueToInformerGVK.merge(glue.getMetadata().getName(), new HashSet<>(Set.of(gvk)),
        (s1, s2) -> {
          s1.addAll(s2);
          return s1;
        });
  }

  private boolean unmarkEventSource(GroupVersionKind gvk,
      Glue glue) {
    var gvkSet = glueToInformerGVK.get(glue.getMetadata().getName());
    gvkSet.remove(gvk);
    var es = gvkOfInformerToGlue.get(gvk);
    es.remove(workflowId(glue));
    return es.isEmpty();
  }

  private String workflowId(Glue glue) {
    return glue.getMetadata().getName() + "#" + glue.getMetadata().getNamespace();
  }

}

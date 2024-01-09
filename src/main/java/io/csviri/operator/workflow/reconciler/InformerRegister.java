package io.csviri.operator.workflow.reconciler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.workflow.Utils;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
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

  public synchronized void deRegisterInformerOnWorkflowChange(Context<Workflow> context,
      Workflow primary) {
    var registeredGVKSet =
        new HashSet<>(workflowToInformerGVK.get(primary.getMetadata().getName()));
    var currentGVKSet = primary.getSpec().getResources().stream()
        .map(Utils::getGVK)
        .collect(Collectors.toSet());
    registeredGVKSet.removeAll(currentGVKSet);
    registeredGVKSet.forEach(gvk -> {
      log.debug("De-registering Informer on Workflow change for workflow: {} gvk: {}", primary,
          gvk);
      deRegisterInformer(gvk, primary, context);
    });
  }

  // todo tests + remake WO to use related resources
  public void registerInformerForRelatedResource(Context<Workflow> context, Workflow workflow,
      GroupVersionKind gvk, String namespace, List<String> names) {

    relatedResourceMappers.putIfAbsent(gvk, new RelatedResourceSecondaryToPrimaryMapper());
    var mapper = relatedResourceMappers.get(gvk);
    mapper.addResourceIDMapping(
        names.stream().map(n -> new ResourceID(n, namespace)).collect(Collectors.toSet()),
        ResourceID.fromResource(workflow));

    registerInformer(context, workflow, gvk,
        () -> new InformerEventSource<>(InformerConfiguration.<GenericKubernetesResource>from(gvk)
            .withSecondaryToPrimaryMapper(mapper)
            .build(), context.eventSourceRetriever().eventSourceContextForDynamicRegistration()));
  }

  public void registerInformer(Context<Workflow> context, Workflow workflow, GroupVersionKind gvk,
      Supplier<InformerEventSource<GenericKubernetesResource, Workflow>> newEventSource) {
    registerInformer(context, workflow, gvk, newEventSource, null);
  }

  @SuppressWarnings("unchecked")
  public void registerInformer(Context<Workflow> context, Workflow workflow, GroupVersionKind gvk,
      Supplier<InformerEventSource<GenericKubernetesResource, Workflow>> newEventSourceSupplier,
      Consumer<InformerEventSource<GenericKubernetesResource, Workflow>> existingInformerConsumer) {

    // mark is synchronized, even if the deRegistration happens instantly after mark it won't
    // deregister the informer since an additional is marked. This, makes sure that start - possibly
    // long blocking operation - not happens in a synchronized block
    InformerEventSource<GenericKubernetesResource, Workflow> es;
    synchronized (this) {
      markEventSource(gvk, workflow);
      es = getInformerEventSource(context, gvk).map(e -> {
        log.debug("Found event source for: {}", gvk);
        if (existingInformerConsumer != null) {
          existingInformerConsumer.accept(e);
        }
        return e;
      }).orElse(null);
    }
    if (es == null) {
      log.debug("Adding new event source for: {}", gvk);
      var newEventSource = newEventSourceSupplier.get();
      var resultEventSource =
          context.eventSourceRetriever().dynamicallyRegisterEventSource(gvk.toString(),
              newEventSource);
      if (resultEventSource != newEventSource) {
        existingInformerConsumer
            .accept((InformerEventSource<GenericKubernetesResource, Workflow>) resultEventSource);
      }
    }
  }

  public synchronized void deRegisterInformer(GroupVersionKind groupVersionKind,
      Workflow primary,
      Context<Workflow> context) {
    var lastForGVK = unmarkEventSource(groupVersionKind, primary);
    if (lastForGVK) {
      context.eventSourceRetriever().dynamicallyDeRegisterEventSource(groupVersionKind.toString());
    }
  }

  private synchronized void markEventSource(GroupVersionKind gvk,
      Workflow workflow) {

    gvkOfInformerToWorkflow.merge(gvk, new HashSet<>(Set.of(workflowId(workflow))),
        (s1, s2) -> {
          s1.addAll(s2);
          return s1;
        });
    workflowToInformerGVK.merge(workflow.getMetadata().getName(), new HashSet<>(Set.of(gvk)),
        (s1, s2) -> {
          s1.addAll(s2);
          return s1;
        });
  }

  private boolean unmarkEventSource(GroupVersionKind gvk,
      Workflow workflow) {

    var gvkSet = workflowToInformerGVK.get(workflow.getMetadata().getName());
    gvkSet.remove(gvk);
    var es = gvkOfInformerToWorkflow.get(gvk);
    es.remove(workflowId(workflow));
    return es.isEmpty();
  }

  private String workflowId(Workflow workflow) {
    return workflow.getMetadata().getName() + "#" + workflow.getMetadata().getNamespace();
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

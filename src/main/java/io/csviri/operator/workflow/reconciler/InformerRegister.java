package io.csviri.operator.workflow.reconciler;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

class InformerRegister {

  private static final Logger log = LoggerFactory.getLogger(InformerRegister.class);

  private final Map<String, Set<String>> registeredEventSourcesForGVK = new HashMap<>();

  public void registerInformer(Context<Workflow> context, Workflow workflow, GroupVersionKind gvk,
      Supplier<InformerEventSource<GenericKubernetesResource, Workflow>> newEventSource) {
    registerInformer(context, workflow, gvk, newEventSource, null);
  }

  @SuppressWarnings("unchecked")
  public void registerInformer(Context<Workflow> context, Workflow workflow, GroupVersionKind gvk,
      Supplier<InformerEventSource<GenericKubernetesResource, Workflow>> newEventSource,
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
      }).orElseGet(() -> {
        log.debug("Adding new event source for: {}", gvk);
        return (InformerEventSource<GenericKubernetesResource, Workflow>) context
            .eventSourceRetriever()
            .dynamicallyRegisterEventSource(gvk.toString(), newEventSource.get(), false);
      });
    }
    es.start();
  }

  public synchronized void deRegisterEventSource(GroupVersionKind groupVersionKind,
      Workflow primary,
      Context<Workflow> context) {
    var lastForGVK = unmarkEventSource(groupVersionKind, primary);
    if (lastForGVK) {
      context.eventSourceRetriever().dynamicallyDeRegisterEventSource(groupVersionKind.toString());
    }
  }

  private synchronized void markEventSource(GroupVersionKind gvk,
      Workflow workflow) {
    registeredEventSourcesForGVK.merge(gvk.toString(), new HashSet<>(Set.of(workflowId(workflow))),
        (s1, s2) -> {
          s1.addAll(s2);
          return s1;
        });
  }

  private boolean unmarkEventSource(GroupVersionKind gvk,
      Workflow workflow) {
    var key = gvk.toString();
    var es = registeredEventSourcesForGVK.get(key);
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

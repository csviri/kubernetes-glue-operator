package io.csviri.operator.workflow.reconciler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.csviri.operator.workflow.Utils;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

class InformerRegister {

  private final Map<String, Set<String>> registeredEventSourcesForGVK = new ConcurrentHashMap<>();

  public void registerInformer(Context<Workflow> context, Workflow workflow, GroupVersionKind gvk,
      Supplier<InformerEventSource<GenericKubernetesResource, Workflow>> newEventSource) {
    registerInformer(context, workflow, gvk, newEventSource, null);
  }

  public void registerInformer(Context<Workflow> context, Workflow workflow, GroupVersionKind gvk,
      Supplier<InformerEventSource<GenericKubernetesResource, Workflow>> newEventSource,
      Consumer<InformerEventSource<GenericKubernetesResource, Workflow>> existingInformerConsumer) {

    Utils.getInformerEventSource(context, gvk).ifPresentOrElse(es -> {
      // make sure it is already started up (thus synced)
      es.start();
      if (existingInformerConsumer != null) {
        existingInformerConsumer.accept(es);
      }
    },
        () -> context.eventSourceRetriever()
            .dynamicallyRegisterEventSource(gvk.toString(), newEventSource.get()));
    markEventSource(gvk, workflow);
  }

  public void deRegisterEventSource(GroupVersionKind groupVersionKind, Workflow primary,
      Context<Workflow> context) {
    var lastForGVK = unmarkEventSource(groupVersionKind, primary);
    if (lastForGVK) {
      context.eventSourceRetriever().dynamicallyDeRegisterEventSource(groupVersionKind.toString());
    }
  }

  // this should be idempotent
  private synchronized void markEventSource(GroupVersionKind gvk,
      Workflow workflow) {
    registeredEventSourcesForGVK.merge(gvk.toString(), new HashSet<>(Set.of(workflowId(workflow))),
        (s1, s2) -> {
          s1.addAll(s2);
          return s1;
        });
  }

  private synchronized boolean unmarkEventSource(GroupVersionKind gvk,
      Workflow workflow) {
    var key = gvk.toString();
    var es = registeredEventSourcesForGVK.get(key);
    es.remove(workflowId(workflow));
    return es.isEmpty();
  }

  private String workflowId(Workflow workflow) {
    return workflow.getMetadata().getName() + "#" + workflow.getMetadata().getNamespace();
  }

}

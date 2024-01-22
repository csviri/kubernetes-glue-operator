package io.csviri.operator.resourceflow.reconciler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.resourceflow.customresource.operator.ResourceFlowOperator;
import io.csviri.operator.resourceflow.customresource.operator.ResourceFlowOperatorSpec;
import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlow;
import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlowSpec;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

@ControllerConfiguration
public class ResourceFlowOperatorReconciler
    implements Reconciler<ResourceFlowOperator>, EventSourceInitializer<ResourceFlowOperator>,
    Cleaner<ResourceFlowOperator> {

  private static final Logger log = LoggerFactory.getLogger(ResourceFlowOperatorReconciler.class);

  public static final String WORKFLOW_LABEL_KEY = "foroperator";
  public static final String WORKFLOW_LABEL_VALUE = "true";
  public static final String WATCH_PREFIX = "workflow-operator-watch-cr/";

  public static final String WATCH_GROUP = WATCH_PREFIX + "group";
  public static final String WATCH_VERSION = WATCH_PREFIX + "version";
  public static final String WATCH_KIND = WATCH_PREFIX + "kind";
  public static final String WATCH_NAME = WATCH_PREFIX + "name";

  private InformerEventSource<ResourceFlow, ResourceFlowOperator> workflowEventSource;

  @Override
  public UpdateControl<ResourceFlowOperator> reconcile(ResourceFlowOperator resourceFlowOperator,
      Context<ResourceFlowOperator> context) {

    log.debug("Reconciling: {} in namespace: {}", resourceFlowOperator.getMetadata().getName(),
        resourceFlowOperator.getMetadata().getNamespace());

    var targetCREventSource = getOrRegisterCustomResourceEventSource(resourceFlowOperator, context);
    targetCREventSource.list().forEach(cr -> {
      var workFlow = workflowEventSource
          .get(new ResourceID(workflowName(cr), cr.getMetadata().getNamespace()));
      var targetWorkflow = createWorkflow(cr, resourceFlowOperator);
      if (workFlow.isEmpty()) {
        context.getClient().resource(targetWorkflow).create();
      } else if (!match(workFlow.orElseThrow(), targetWorkflow)) {
        context.getClient().resource(targetWorkflow).update();
      }
    });

    return UpdateControl.noUpdate();
  }

  private boolean match(ResourceFlow actualResourceFlow, ResourceFlow targetResourceFlow) {
    // for now cannot change watched resource, coming with related resources
    return actualResourceFlow.getSpec().equals(targetResourceFlow.getSpec());
  }

  private ResourceFlow createWorkflow(GenericKubernetesResource cr,
      ResourceFlowOperator resourceFlowOperator) {
    var res = new ResourceFlow();
    Map<String, String> annotation = new HashMap<>();
    GroupVersionKind gvk = new GroupVersionKind(cr.getApiVersion(), cr.getKind());
    annotation.put(WATCH_GROUP, gvk.getGroup());
    annotation.put(WATCH_VERSION, gvk.getVersion());
    annotation.put(WATCH_KIND, gvk.getKind());
    annotation.put(WATCH_NAME, cr.getMetadata().getName());

    res.setMetadata(new ObjectMetaBuilder()
        .withAnnotations(annotation)
        .withName(workflowName(cr))
        .withNamespace(cr.getMetadata().getNamespace())
        .withLabels(Map.of(WORKFLOW_LABEL_KEY, WORKFLOW_LABEL_VALUE))
        .build());
    res.setSpec(toWorkflowSpec(resourceFlowOperator.getSpec()));
    res.addOwnerReference(cr);
    return res;
  }

  private ResourceFlowSpec toWorkflowSpec(ResourceFlowOperatorSpec spec) {
    var res = new ResourceFlowSpec();
    res.setResources(spec.getResources());
    return res;
  }

  private InformerEventSource<GenericKubernetesResource, ResourceFlowOperator> getOrRegisterCustomResourceEventSource(
      ResourceFlowOperator resourceFlowOperator, Context<ResourceFlowOperator> context) {
    var spec = resourceFlowOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getParent().getApiVersion(), spec.getParent().getKind());
    InformerEventSource<GenericKubernetesResource, ResourceFlowOperator> es;
    // note that this allows just one operator per gvk (what is limitation but ok for now)
    try {
      es = (InformerEventSource<GenericKubernetesResource, ResourceFlowOperator>) context
          .eventSourceRetriever()
          .getResourceEventSourceFor(GenericKubernetesResource.class, gvk.toString());
      es.start();
    } catch (IllegalArgumentException e) {
      es = new InformerEventSource<>(InformerConfiguration.from(gvk,
          context.eventSourceRetriever().eventSourceContextForDynamicRegistration())
          .withSecondaryToPrimaryMapper(
              resource -> Set.of(ResourceID.fromResource(resourceFlowOperator)))
          .build(), context.eventSourceRetriever().eventSourceContextForDynamicRegistration());
      context.eventSourceRetriever().dynamicallyRegisterEventSource(gvk.toString(), es);
    }
    return es;
  }

  @Override
  public Map<String, EventSource> prepareEventSources(
      EventSourceContext<ResourceFlowOperator> eventSourceContext) {
    workflowEventSource = new InformerEventSource<>(
        InformerConfiguration.from(ResourceFlow.class, eventSourceContext)
            .withLabelSelector(WORKFLOW_LABEL_KEY + "=" + WORKFLOW_LABEL_VALUE)
            .build(),
        eventSourceContext);
    return EventSourceInitializer.nameEventSources(workflowEventSource);
  }

  @Override
  public DeleteControl cleanup(ResourceFlowOperator resourceFlowOperator,
      Context<ResourceFlowOperator> context) {
    var spec = resourceFlowOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getParent().getApiVersion(), spec.getParent().getKind());
    context.eventSourceRetriever().dynamicallyDeRegisterEventSource(gvk.toString());
    return DeleteControl.defaultDelete();
  }

  private String workflowName(GenericKubernetesResource cr) {
    return KubernetesResourceUtil.sanitizeName(cr.getMetadata().getName() + "-" + cr.getKind());
  }

}

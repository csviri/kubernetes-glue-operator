package io.csviri.operator.resourceflow.reconciler.operator;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.resourceflow.customresource.operator.ResourceFlowOperator;
import io.csviri.operator.resourceflow.customresource.operator.ResourceFlowOperatorSpec;
import io.csviri.operator.resourceflow.customresource.resourceflow.RelatedResourceSpec;
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
  public static final String PARENT_RELATED_RESOURCE_NAME = "parent";

  private InformerEventSource<ResourceFlow, ResourceFlowOperator> resourceFlowEventSource;

  @Override
  public UpdateControl<ResourceFlowOperator> reconcile(ResourceFlowOperator resourceFlowOperator,
      Context<ResourceFlowOperator> context) {

    log.debug("Reconciling: {} in namespace: {}", resourceFlowOperator.getMetadata().getName(),
        resourceFlowOperator.getMetadata().getNamespace());

    var targetCREventSource = getOrRegisterCustomResourceEventSource(resourceFlowOperator, context);
    targetCREventSource.list().forEach(cr -> {
      var resourceFlow = resourceFlowEventSource
          .get(new ResourceID(workflowName(cr), cr.getMetadata().getNamespace()));
      var targetWorkflow = createWorkflow(cr, resourceFlowOperator);
      if (resourceFlow.isEmpty()) {
        context.getClient().resource(targetWorkflow).create();
      } else if (!match(resourceFlow.orElseThrow(), targetWorkflow)) {
        context.getClient().resource(targetWorkflow).update();
      }
    });

    return UpdateControl.noUpdate();
  }

  private boolean match(ResourceFlow actualResourceFlow, ResourceFlow targetResourceFlow) {
    // todo match related resource
    // for now cannot change watched resource, coming with related resources
    return actualResourceFlow.getSpec().getResources()
        .equals(targetResourceFlow.getSpec().getResources());
  }

  private ResourceFlow createWorkflow(GenericKubernetesResource cr,
      ResourceFlowOperator resourceFlowOperator) {
    var res = new ResourceFlow();

    res.setMetadata(new ObjectMetaBuilder()
        .withName(workflowName(cr))
        .withNamespace(cr.getMetadata().getNamespace())
        .withLabels(Map.of(WORKFLOW_LABEL_KEY, WORKFLOW_LABEL_VALUE))
        .build());
    res.setSpec(toWorkflowSpec(resourceFlowOperator.getSpec()));

    var parent = resourceFlowOperator.getSpec().getParent();
    RelatedResourceSpec parentRelatedSpec = new RelatedResourceSpec();
    parentRelatedSpec.setName(PARENT_RELATED_RESOURCE_NAME);
    parentRelatedSpec.setApiVersion(parent.getApiVersion());
    parentRelatedSpec.setKind(parent.getKind());
    parentRelatedSpec.setResourceNames(List.of(cr.getMetadata().getName()));
    parentRelatedSpec.setNamespace(cr.getMetadata().getNamespace());

    res.getSpec().getRelatedResources().add(parentRelatedSpec);

    res.addOwnerReference(cr);
    return res;
  }

  private ResourceFlowSpec toWorkflowSpec(ResourceFlowOperatorSpec spec) {
    var res = new ResourceFlowSpec();
    res.setResources(new ArrayList<>(spec.getResources()));
    res.setRelatedResources(new ArrayList<>(spec.getRelatedResources()));
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
    resourceFlowEventSource = new InformerEventSource<>(
        InformerConfiguration.from(ResourceFlow.class, eventSourceContext)
            .withLabelSelector(WORKFLOW_LABEL_KEY + "=" + WORKFLOW_LABEL_VALUE)
            .build(),
        eventSourceContext);
    return EventSourceInitializer.nameEventSources(resourceFlowEventSource);
  }

  @Override
  public DeleteControl cleanup(ResourceFlowOperator resourceFlowOperator,
      Context<ResourceFlowOperator> context) {
    var spec = resourceFlowOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getParent().getApiVersion(), spec.getParent().getKind());
    context.eventSourceRetriever().dynamicallyDeRegisterEventSource(gvk.toString());
    return DeleteControl.defaultDelete();
  }

  private static String workflowName(GenericKubernetesResource cr) {
    return KubernetesResourceUtil.sanitizeName(cr.getMetadata().getName() + "-" + cr.getKind());
  }

}

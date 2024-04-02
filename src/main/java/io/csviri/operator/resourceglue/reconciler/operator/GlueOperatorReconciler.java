package io.csviri.operator.resourceglue.reconciler.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.resourceglue.customresource.glue.Glue;
import io.csviri.operator.resourceglue.customresource.glue.RelatedResourceSpec;
import io.csviri.operator.resourceglue.customresource.glue.ResourceGlueSpec;
import io.csviri.operator.resourceglue.customresource.operator.GlueOperator;
import io.csviri.operator.resourceglue.customresource.operator.ResourceGlueOperatorSpec;
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
public class GlueOperatorReconciler
    implements Reconciler<GlueOperator>, EventSourceInitializer<GlueOperator>,
    Cleaner<GlueOperator> {

  private static final Logger log = LoggerFactory.getLogger(GlueOperatorReconciler.class);

  public static final String GLUE_LABEL_KEY = "foroperator";
  public static final String GLUE_LABEL_VALUE = "true";
  public static final String PARENT_RELATED_RESOURCE_NAME = "parent";

  private InformerEventSource<Glue, GlueOperator> resourceFlowEventSource;

  @Override
  public UpdateControl<GlueOperator> reconcile(GlueOperator glueOperator,
      Context<GlueOperator> context) {

    log.info("Reconciling GlueOperator {} in namespace: {}", glueOperator.getMetadata().getName(),
        glueOperator.getMetadata().getNamespace());

    var targetCREventSource = getOrRegisterCustomResourceEventSource(glueOperator, context);
    targetCREventSource.list().forEach(cr -> {
      var actualResourceFlow = resourceFlowEventSource
          .get(new ResourceID(glueName(cr), cr.getMetadata().getNamespace()));
      var desiredResourceFlow = createResourceFlow(cr, glueOperator);
      if (actualResourceFlow.isEmpty()) {
        context.getClient().resource(desiredResourceFlow).serverSideApply();
      } else if (!actualResourceFlow.orElseThrow().getSpec()
          .equals(desiredResourceFlow.getSpec())) {
        log.debug("Updating resource from for operator name: {} cr: {} namespace: {}",
            glueOperator.getMetadata().getName(),
            cr.getMetadata().getName(),
            glueOperator.getMetadata().getNamespace());
        context.getClient().resource(desiredResourceFlow).serverSideApply();
      }
    });

    return UpdateControl.noUpdate();
  }

  private Glue createResourceFlow(GenericKubernetesResource targetParentResource,
      GlueOperator glueOperator) {
    var glue = new Glue();

    glue.setMetadata(new ObjectMetaBuilder()
        .withName(glueName(targetParentResource))
        .withNamespace(targetParentResource.getMetadata().getNamespace())
        .withLabels(Map.of(GLUE_LABEL_KEY, GLUE_LABEL_VALUE))
        .build());
    glue.setSpec(toWorkflowSpec(glueOperator.getSpec()));

    var parent = glueOperator.getSpec().getParent();
    RelatedResourceSpec parentRelatedSpec = new RelatedResourceSpec();
    parentRelatedSpec.setName(PARENT_RELATED_RESOURCE_NAME);
    parentRelatedSpec.setApiVersion(parent.getApiVersion());
    parentRelatedSpec.setKind(parent.getKind());
    parentRelatedSpec.setResourceNames(List.of(targetParentResource.getMetadata().getName()));
    parentRelatedSpec.setNamespace(targetParentResource.getMetadata().getNamespace());

    glue.getSpec().getRelatedResources().add(parentRelatedSpec);

    glue.addOwnerReference(targetParentResource);
    return glue;
  }

  private ResourceGlueSpec toWorkflowSpec(ResourceGlueOperatorSpec spec) {
    var res = new ResourceGlueSpec();
    res.setResources(new ArrayList<>(spec.getResources()));
    res.setRelatedResources(new ArrayList<>(spec.getRelatedResources()));
    return res;
  }

  private InformerEventSource<GenericKubernetesResource, GlueOperator> getOrRegisterCustomResourceEventSource(
      GlueOperator glueOperator, Context<GlueOperator> context) {
    var spec = glueOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getParent().getApiVersion(), spec.getParent().getKind());
    InformerEventSource<GenericKubernetesResource, GlueOperator> es;
    // note that this allows just one operator per gvk (what is limitation but ok for now)
    try {
      es = (InformerEventSource<GenericKubernetesResource, GlueOperator>) context
          .eventSourceRetriever()
          .getResourceEventSourceFor(GenericKubernetesResource.class, gvk.toString());
      es.start();
    } catch (IllegalArgumentException e) {
      es = new InformerEventSource<>(InformerConfiguration.from(gvk,
          context.eventSourceRetriever().eventSourceContextForDynamicRegistration())
          .withSecondaryToPrimaryMapper(
              resource -> Set.of(ResourceID.fromResource(glueOperator)))
          .build(), context.eventSourceRetriever().eventSourceContextForDynamicRegistration());
      context.eventSourceRetriever().dynamicallyRegisterEventSource(gvk.toString(), es);
    }
    return es;
  }

  @Override
  public Map<String, EventSource> prepareEventSources(
      EventSourceContext<GlueOperator> eventSourceContext) {
    resourceFlowEventSource = new InformerEventSource<>(
        InformerConfiguration.from(Glue.class, eventSourceContext)
            .withLabelSelector(GLUE_LABEL_KEY + "=" + GLUE_LABEL_VALUE)
            .build(),
        eventSourceContext);
    return EventSourceInitializer.nameEventSources(resourceFlowEventSource);
  }

  @Override
  public DeleteControl cleanup(GlueOperator glueOperator,
      Context<GlueOperator> context) {
    var spec = glueOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getParent().getApiVersion(), spec.getParent().getKind());
    context.eventSourceRetriever().dynamicallyDeRegisterEventSource(gvk.toString());
    return DeleteControl.defaultDelete();
  }

  private static String glueName(GenericKubernetesResource cr) {
    return KubernetesResourceUtil.sanitizeName(cr.getMetadata().getName() + "-" + cr.getKind());
  }

}

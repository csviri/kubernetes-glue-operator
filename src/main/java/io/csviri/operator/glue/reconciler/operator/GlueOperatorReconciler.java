package io.csviri.operator.glue.reconciler.operator;

import java.util.*;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.glue.ControllerConfig;
import io.csviri.operator.glue.GlueException;
import io.csviri.operator.glue.customresource.glue.Glue;
import io.csviri.operator.glue.customresource.glue.GlueSpec;
import io.csviri.operator.glue.customresource.glue.RelatedResourceSpec;
import io.csviri.operator.glue.customresource.operator.GlueOperator;
import io.csviri.operator.glue.customresource.operator.GlueOperatorSpec;
import io.csviri.operator.glue.customresource.operator.ResourceFlowOperatorStatus;
import io.csviri.operator.glue.reconciler.ValidationAndErrorHandler;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import static io.csviri.operator.glue.reconciler.glue.GlueReconciler.GLUE_RECONCILER_NAME;

@ControllerConfiguration(name = GlueOperatorReconciler.GLUE_OPERATOR_RECONCILER_NAME)
public class GlueOperatorReconciler
    implements Reconciler<GlueOperator>, EventSourceInitializer<GlueOperator>,
    Cleaner<GlueOperator>, ErrorStatusHandler<GlueOperator> {

  private static final Logger log = LoggerFactory.getLogger(GlueOperatorReconciler.class);

  public static final String GLUE_LABEL_KEY = "foroperator";
  public static final String GLUE_LABEL_VALUE = "true";
  public static final String PARENT_RELATED_RESOURCE_NAME = "parent";
  public static final String GLUE_OPERATOR_RECONCILER_NAME = "glue-operator";

  @Inject
  ValidationAndErrorHandler validationAndErrorHandler;

  @ConfigProperty(name = "quarkus.operator-sdk.controllers." + GLUE_RECONCILER_NAME + ".selector")
  Optional<String> glueLabelSelector;

  @Inject
  ControllerConfig controllerConfig;

  private Map<String, String> defaultGlueLabels;

  private InformerEventSource<Glue, GlueOperator> glueEventSource;

  @PostConstruct
  void init() {
    defaultGlueLabels = initDefaultLabelsToAddToGlue();
  }

  @Override
  public UpdateControl<GlueOperator> reconcile(GlueOperator glueOperator,
      Context<GlueOperator> context) {

    log.info("Reconciling GlueOperator {} in namespace: {}", glueOperator.getMetadata().getName(),
        glueOperator.getMetadata().getNamespace());

    validationAndErrorHandler.checkIfNamesAreUnique(glueOperator.getSpec());

    var targetCREventSource = getOrRegisterCustomResourceEventSource(glueOperator, context);
    targetCREventSource.list().forEach(cr -> {
      var actualResourceFlow = glueEventSource
          .get(new ResourceID(glueName(cr.getMetadata().getName(), cr.getKind()),
              cr.getMetadata().getNamespace()));
      var desiredResourceFlow = createGlue(cr, glueOperator);
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

  private Glue createGlue(GenericKubernetesResource targetParentResource,
      GlueOperator glueOperator) {
    var glue = new Glue();

    glue.setMetadata(new ObjectMetaBuilder()
        .withName(
            glueName(targetParentResource.getMetadata().getName(), targetParentResource.getKind()))
        .withNamespace(targetParentResource.getMetadata().getNamespace())
        .withLabels(Map.of(GLUE_LABEL_KEY, GLUE_LABEL_VALUE))
        .build());
    glue.setSpec(toWorkflowSpec(glueOperator.getSpec()));

    if (!defaultGlueLabels.isEmpty()) {
      glue.getMetadata().getLabels().putAll(defaultGlueLabels);
    }

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

  private GlueSpec toWorkflowSpec(GlueOperatorSpec spec) {
    var res = new GlueSpec();
    res.setChildResources(new ArrayList<>(spec.getChildResources()));
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
      var configBuilder = InformerConfiguration.from(gvk,
          context.eventSourceRetriever().eventSourceContextForDynamicRegistration())
          .withSecondaryToPrimaryMapper(
              resource -> Set.of(ResourceID.fromResource(glueOperator)));

      if (spec.getParent().getLabelSelector() != null) {
        configBuilder.withLabelSelector(spec.getParent().getLabelSelector());
      }

      es = new InformerEventSource<>(configBuilder.build(),
          context.eventSourceRetriever().eventSourceContextForDynamicRegistration());
      context.eventSourceRetriever().dynamicallyRegisterEventSource(gvk.toString(), es);
    }
    return es;
  }

  @Override
  public Map<String, EventSource> prepareEventSources(
      EventSourceContext<GlueOperator> eventSourceContext) {
    glueEventSource = new InformerEventSource<>(
        InformerConfiguration.from(Glue.class, eventSourceContext)
            .withLabelSelector(GLUE_LABEL_KEY + "=" + GLUE_LABEL_VALUE)
            .build(),
        eventSourceContext);
    return EventSourceInitializer.nameEventSources(glueEventSource);
  }

  @Override
  public ErrorStatusUpdateControl<GlueOperator> updateErrorStatus(GlueOperator resource,
      Context<GlueOperator> context, Exception e) {
    if (resource.getStatus() == null) {
      resource.setStatus(new ResourceFlowOperatorStatus());
    }
    return validationAndErrorHandler.updateStatusErrorMessage(e, resource);
  }

  @Override
  public DeleteControl cleanup(GlueOperator glueOperator,
      Context<GlueOperator> context) {
    var spec = glueOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getParent().getApiVersion(), spec.getParent().getKind());
    context.eventSourceRetriever().dynamicallyDeRegisterEventSource(gvk.toString());
    return DeleteControl.defaultDelete();
  }

  public static String glueName(String name, String kind) {
    return KubernetesResourceUtil.sanitizeName(name + "-" + kind);
  }

  private Map<String, String> initDefaultLabelsToAddToGlue() {
    Map<String, String> res = new HashMap<>();
    if (!controllerConfig.glueOperatorManagedGlueLabel().isEmpty()) {
      res.putAll(controllerConfig.glueOperatorManagedGlueLabel());
    } else {
      glueLabelSelector.ifPresent(ls -> {
        if (ls.contains(",") || ls.contains("(")) {
          throw new GlueException(
              "Glue reconciler label selector contains non-simple label selector: " + ls +
                  ". Specify Glue label selector in simple form ('key=value' or 'key') " +
                  "or configure 'glue.operator.glue-operator-managed-glue-label'");
        }
        String[] labelSelectorParts = ls.split("=");
        if (labelSelectorParts.length > 2) {
          throw new GlueException("Invalid label selector: " + ls);
        }
        if (labelSelectorParts.length == 1) {
          res.put(labelSelectorParts[0], "");
        } else {
          res.put(labelSelectorParts[0], labelSelectorParts[1]);
        }
      });
    }
    return res;
  }

}

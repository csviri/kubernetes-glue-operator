package io.csviri.operator.resourceglue.reconciler.glue;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.resourceglue.Utils;
import io.csviri.operator.resourceglue.conditions.JavaScripCondition;
import io.csviri.operator.resourceglue.conditions.PodsReadyCondition;
import io.csviri.operator.resourceglue.customresource.glue.DependentResourceSpec;
import io.csviri.operator.resourceglue.customresource.glue.Glue;
import io.csviri.operator.resourceglue.customresource.glue.condition.ConditionSpec;
import io.csviri.operator.resourceglue.customresource.glue.condition.JavaScriptConditionSpec;
import io.csviri.operator.resourceglue.customresource.glue.condition.PodsReadyConditionSpec;
import io.csviri.operator.resourceglue.dependent.GCGenericDependentResource;
import io.csviri.operator.resourceglue.dependent.GenericDependentResource;
import io.csviri.operator.resourceglue.dependent.GenericResourceDiscriminator;
import io.csviri.operator.resourceglue.templating.GenericTemplateHandler;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.KubernetesResourceDeletedCondition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

@ControllerConfiguration
public class GlueReconciler implements Reconciler<Glue>, Cleaner<Glue> {

  private static final Logger log = LoggerFactory.getLogger(GlueReconciler.class);
  public static final String DEPENDENT_NAME_ANNOTATION_KEY = "io.csviri.operator.resourceflow/name";

  private final KubernetesResourceDeletedCondition deletePostCondition =
      new KubernetesResourceDeletedCondition();
  private final InformerRegister informerRegister = new InformerRegister();
  private final GenericTemplateHandler genericTemplateHandler = new GenericTemplateHandler();

  @Override
  public UpdateControl<Glue> reconcile(Glue primary,
      Context<Glue> context) {

    registerRelatedResourceInformers(context, primary);
    if (ownersBeingDeleted(primary, context)) {
      return UpdateControl.noUpdate();
    }
    var actualWorkflow = buildWorkflowAndRegisterInformers(primary, context);
    var result = actualWorkflow.reconcile(primary, context);
    cleanupRemovedResourcesFromWorkflow(context, primary);
    informerRegister.deRegisterInformerOnResourceFlowChange(context, primary);
    result.throwAggregateExceptionIfErrorsPresent();
    return UpdateControl.noUpdate();
  }

  /**
   * If a parent gets deleted, the glue is reconciled still, but we don't want that in that case.
   * Glue us deleted / marked for deleted eventually by the garbage collector but want to make the
   * best effort to prevent that.
   */
  private boolean ownersBeingDeleted(Glue primary, Context<Glue> context) {
    if (primary.getMetadata().getOwnerReferences().isEmpty()) {
      return false;
    }

    var res = !primary.getMetadata().getOwnerReferences().stream().anyMatch(or -> {
      var gvk = new GroupVersionKind(or.getApiVersion(), or.getKind());
      var es = (InformerEventSource<GenericKubernetesResource, Glue>) context.eventSourceRetriever()
          .getResourceEventSourceFor(GenericKubernetesResource.class, gvk.toString());
      var resource = es.get(new ResourceID(or.getName(), primary.getMetadata().getNamespace()));
      return resource.map(r -> r.getMetadata().getDeletionTimestamp() == null).orElse(false);

    });
    if (res) {
      log.debug("Skipping reconciliation since glue owners being deleted, name: {} namespace: {}",
          primary.getMetadata().getName(),
          primary.getMetadata().getNamespace());
    }
    return res;
  }

  @Override
  public DeleteControl cleanup(Glue primary, Context<Glue> context) {
    // todo if related resource referenced to name the resource (name / namespace) the related
    // resource might be deleted
    // at this point - shall we add finalizer?

    var actualWorkflow = buildWorkflowAndRegisterInformers(primary, context);

    var result = actualWorkflow.cleanup(primary, context);
    result.throwAggregateExceptionIfErrorsPresent();

    if (!result.allPostConditionsMet()) {
      return DeleteControl.noFinalizerRemoval();
    } else {
      actualWorkflow.getDependentResourcesByNameWithoutActivationCondition().forEach((n, dr) -> {
        var genericDependentResource = (GenericDependentResource) dr;
        informerRegister.deRegisterInformer(genericDependentResource.getGroupVersionKind(),
            primary, context);
      });
      informerRegister.deRegisterInformerForRelatedResources(primary, context);

      return DeleteControl.defaultDelete();
    }
  }

  private void registerRelatedResourceInformers(Context<Glue> context,
      Glue glue) {
    glue.getSpec().getRelatedResources().forEach(r -> {
      informerRegister.registerInformerForRelatedResource(context, glue, r);
    });
  }

  // todo test
  private void cleanupRemovedResourcesFromWorkflow(Context<Glue> context,
      Glue primary) {

    context.getSecondaryResources(GenericKubernetesResource.class).forEach(r -> {
      String dependentName = r.getMetadata().getAnnotations().get(DEPENDENT_NAME_ANNOTATION_KEY);
      // dependent name is null for related resources
      if (dependentName != null && primary.getSpec().getResources().stream()
          .filter(pr -> pr.getName().equals(dependentName)).findAny().isEmpty()) {
        try {
          log.debug("Deleting resource with name: {}", dependentName + "for resource flow: {} "
              + primary.getMetadata().getName());
          context.getClient().resource(r).delete();
        } catch (KubernetesClientException e) {
          // can happen that already deleted, just in cache.
          log.warn("Error during deleting resource on workflow change", e);
        }
      }
    });
  }

  private io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow<Glue> buildWorkflowAndRegisterInformers(
      Glue primary, Context<Glue> context) {
    var builder = new WorkflowBuilder<Glue>();
    Set<String> leafDependentNames = Utils.leafResourceNames(primary);

    Map<String, GenericDependentResource> genericDependentResourceMap = new HashMap<>();
    primary.getSpec().getResources().forEach(spec -> createAndAddDependentToWorkflow(primary,
        context, spec, genericDependentResourceMap, builder,
        leafDependentNames.contains(spec.getName())));

    return builder.build();
  }

  private void createAndAddDependentToWorkflow(Glue primary, Context<Glue> context,
      DependentResourceSpec spec,
      Map<String, GenericDependentResource> genericDependentResourceMap,
      WorkflowBuilder<Glue> builder, boolean leafDependent) {


    var dr = createDependentResource(spec, leafDependent);
    var gvk = dr.getGroupVersionKind();


    dr.setResourceDiscriminator(new GenericResourceDiscriminator(dr.getGroupVersionKind(),
        // name can reference related resources todo doc
        genericTemplateHandler.processTemplate(Utils.getName(spec), primary, context),
        // todo test processing ns as template
        Utils.getNamespace(spec).map(ns -> genericTemplateHandler
            .processTemplate(Utils.getName(spec), primary, context))
            .orElse(null)));

    var es = informerRegister.registerInformer(context, gvk, primary);
    dr.configureWith(es);

    builder.addDependentResource(dr);
    spec.getDependsOn().forEach(s -> builder.dependsOn(genericDependentResourceMap.get(s)));
    // if a resources does not depend on another there is no reason to add cleanup condition
    if (!spec.getDependsOn().isEmpty()) {
      builder.withDeletePostcondition(deletePostCondition);
    }
    genericDependentResourceMap.put(spec.getName(), dr);

    Optional.ofNullable(spec.getReadyPostCondition())
        .ifPresent(c -> builder.withReadyPostcondition(toCondition(c)));
    Optional.ofNullable(spec.getCondition())
        .ifPresent(c -> builder.withReconcilePrecondition(toCondition(c)));
    Optional.ofNullable(spec.getDeletePostCondition())
        .ifPresent(c -> builder.withDeletePostcondition(toCondition(c)));
  }

  private static GenericDependentResource createDependentResource(DependentResourceSpec spec,
      boolean leafDependent) {
    if (leafDependent) {
      return spec.getResourceTemplate() != null
          ? new GCGenericDependentResource(spec.getResourceTemplate(), spec.getName())
          : new GCGenericDependentResource(spec.getResource(), spec.getName());
    } else {
      return spec.getResourceTemplate() != null
          ? new GenericDependentResource(spec.getResourceTemplate(), spec.getName())
          : new GenericDependentResource(spec.getResource(), spec.getName());
    }
  }

  @SuppressWarnings({"rawtypes"})
  private Condition toCondition(ConditionSpec condition) {
    if (condition instanceof PodsReadyConditionSpec readyConditionSpec) {
      return new PodsReadyCondition(readyConditionSpec.isNegated());
    } else if (condition instanceof JavaScriptConditionSpec jsCondition) {
      return new JavaScripCondition(jsCondition.getScript());
    }
    throw new IllegalStateException("Unknown condition: " + condition);
  }

}

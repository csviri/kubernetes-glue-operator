package io.csviri.operator.glue.reconciler.glue;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.glue.Utils;
import io.csviri.operator.glue.conditions.JavaScripCondition;
import io.csviri.operator.glue.conditions.ReadyCondition;
import io.csviri.operator.glue.customresource.glue.DependentResourceSpec;
import io.csviri.operator.glue.customresource.glue.Glue;
import io.csviri.operator.glue.customresource.glue.condition.ConditionSpec;
import io.csviri.operator.glue.customresource.glue.condition.JavaScriptConditionSpec;
import io.csviri.operator.glue.customresource.glue.condition.ReadyConditionSpec;
import io.csviri.operator.glue.dependent.GCGenericDependentResource;
import io.csviri.operator.glue.dependent.GenericDependentResource;
import io.csviri.operator.glue.dependent.GenericResourceDiscriminator;
import io.csviri.operator.glue.templating.GenericTemplateHandler;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.KubernetesResourceDeletedCondition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;

import static io.csviri.operator.glue.Utils.getResourceForSSAFrom;
import static io.csviri.operator.glue.reconciler.operator.GlueOperatorReconciler.PARENT_RELATED_RESOURCE_NAME;

@ControllerConfiguration
public class GlueReconciler implements Reconciler<Glue>, Cleaner<Glue> {

  private static final Logger log = LoggerFactory.getLogger(GlueReconciler.class);
  public static final String DEPENDENT_NAME_ANNOTATION_KEY = "io.csviri.operator.resourceflow/name";
  public static final String PARENT_GLUE_FINALIZER_PREFIX = "io.csviri.operator.resourceflow.glue/";

  private final KubernetesResourceDeletedCondition deletePostCondition =
      new KubernetesResourceDeletedCondition();
  private final InformerRegister informerRegister = new InformerRegister();
  private final GenericTemplateHandler genericTemplateHandler = new GenericTemplateHandler();

  /**
   * Handling finalizers for GlueOperator: Glue ids a finalizer to parent, that is necessary since
   * on clean up the resource name might be calculated based on the parents name, and it this way
   * makes sure that parent is not cleaned up until the Glue is cleaned up. The finalizer is removed
   * during cleanup. On Glue side however it is important to make sure that if the parent is deleted
   * glue gets deleted too, this is made sure in the reconcile method for glue explicitly deleting
   * itself.
   */

  @Override
  public UpdateControl<Glue> reconcile(Glue primary,
      Context<Glue> context) {

    log.debug("Reconciling glue. name: {} namespace: {}",
        primary.getMetadata().getName(), primary.getMetadata().getNamespace());



    registerRelatedResourceInformers(context, primary);
    if (deletedGlueIfParentMarkedForDeletion(context, primary)) {
      return UpdateControl.noUpdate();
    }
    addFinalizersToParentResource(primary, context);
    var actualWorkflow = buildWorkflowAndRegisterInformers(primary, context);
    var result = actualWorkflow.reconcile(primary, context);
    cleanupRemovedResourcesFromWorkflow(context, primary);
    informerRegister.deRegisterInformerOnResourceFlowChange(context, primary);
    result.throwAggregateExceptionIfErrorsPresent();
    return UpdateControl.noUpdate();
  }

  private boolean deletedGlueIfParentMarkedForDeletion(Context<Glue> context, Glue primary) {
    var parent = getParentRelatedResource(primary, context);
    if (parent.map(HasMetadata::isMarkedForDeletion).orElse(false)) {
      context.getClient().resource(primary).delete();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public DeleteControl cleanup(Glue primary, Context<Glue> context) {

    var actualWorkflow = buildWorkflowAndRegisterInformers(primary, context);
    var result = actualWorkflow.cleanup(primary, context);
    result.throwAggregateExceptionIfErrorsPresent();

    if (!result.allPostConditionsMet()) {
      return DeleteControl.noFinalizerRemoval();
    } else {
      removeFinalizerForParent(primary, context);
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

    // todo test processing ns not as template
    // todo test processing ns as template
    // name can reference related resources todo doc
    var targetNamespace = Utils.getNamespace(spec).map(ns -> genericTemplateHandler
        .processTemplate(ns, primary, context));
    var resourceInSameNamespaceAsPrimary =
        targetNamespace.map(n -> n.trim().equals(primary.getMetadata().getNamespace().trim()))
            .orElse(true);

    var dr = createDependentResource(spec, leafDependent, resourceInSameNamespaceAsPrimary);
    var gvk = dr.getGroupVersionKind();

    dr.setResourceDiscriminator(new GenericResourceDiscriminator(dr.getGroupVersionKind(),
        genericTemplateHandler.processTemplate(Utils.getName(spec), primary, context),
        targetNamespace.orElse(null)));

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
  }

  private static GenericDependentResource createDependentResource(DependentResourceSpec spec,
      boolean leafDependent, Boolean resourceInSameNamespaceAsPrimary) {
    if (leafDependent && resourceInSameNamespaceAsPrimary) {
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
    if (condition instanceof ReadyConditionSpec readyConditionSpec) {
      return new ReadyCondition(readyConditionSpec.isNegated());
    } else if (condition instanceof JavaScriptConditionSpec jsCondition) {
      return new JavaScripCondition(jsCondition.getScript());
    }
    throw new IllegalStateException("Unknown condition: " + condition);
  }

  private void addFinalizersToParentResource(Glue primary, Context<Glue> context) {
    var parent = getParentRelatedResource(primary, context);

    parent.ifPresent(p -> {
      log.debug("Adding finalizer to parent. Glue name: {} namespace: {}",
          primary.getMetadata().getName(), primary.getMetadata().getNamespace());
      String finalizer = parentFinalizer(primary.getMetadata().getName());
      if (!p.getMetadata().getFinalizers().contains(finalizer)) {
        var res = getResourceForSSAFrom(p);
        res.getMetadata().getFinalizers().add(finalizer);
        patchResource(res, context);
      }
    });
  }

  private void removeFinalizerForParent(Glue primary, Context<Glue> context) {
    var parent = getParentRelatedResource(primary, context);
    parent.ifPresentOrElse(p -> {
      log.debug("Removing finalizer from parent. Glue name: {} namespace: {}",
          primary.getMetadata().getName(), primary.getMetadata().getNamespace());
      String finalizer = parentFinalizer(primary.getMetadata().getName());
      if (p.getMetadata().getFinalizers().contains(finalizer)) {
        var res = getResourceForSSAFrom(p);
        patchResource(res, context);
      }
    }, () -> log.warn(
        "Parent resource expected to be present on cleanup. Glue name: {} namespace: {}",
        primary.getMetadata().getName(), primary.getMetadata().getNamespace()));
  }

  private GenericKubernetesResource patchResource(GenericKubernetesResource res,
      Context<Glue> context) {
    return context.getClient().resource(res)
        .patch(new PatchContext.Builder()
            .withFieldManager(context.getControllerConfiguration().fieldManager())
            .withForce(true)
            .withPatchType(PatchType.SERVER_SIDE_APPLY)
            .build());
  }

  private Optional<GenericKubernetesResource> getParentRelatedResource(Glue primary,
      Context<Glue> context) {
    var parentRelated = primary.getSpec().getRelatedResources().stream()
        .filter(r -> r.getName().equals(PARENT_RELATED_RESOURCE_NAME))
        .findAny();

    return parentRelated.flatMap(r -> {
      var relatedResources = Utils.getRelatedResources(primary, r, context);
      if (relatedResources.size() > 1) {
        throw new IllegalStateException(
            "parent related resource contains more resourceNames for glue name: "
                + primary.getMetadata().getName()
                + " namespace: " + primary.getMetadata().getNamespace());
      }
      if (relatedResources.isEmpty()) {
        return Optional.empty();
      } else {
        return Optional.of(relatedResources.entrySet().iterator().next().getValue());
      }
    });
  }

  private String parentFinalizer(String glueName) {
    return PARENT_GLUE_FINALIZER_PREFIX + glueName;
  }


}

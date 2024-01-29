package io.csviri.operator.resourceflow.reconciler.flow;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.resourceflow.Utils;
import io.csviri.operator.resourceflow.conditions.JavaScripCondition;
import io.csviri.operator.resourceflow.conditions.PodsReadyCondition;
import io.csviri.operator.resourceflow.customresource.resourceflow.DependentResourceSpec;
import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlow;
import io.csviri.operator.resourceflow.customresource.resourceflow.condition.ConditionSpec;
import io.csviri.operator.resourceflow.customresource.resourceflow.condition.JavaScriptConditionSpec;
import io.csviri.operator.resourceflow.customresource.resourceflow.condition.PodsReadyConditionSpec;
import io.csviri.operator.resourceflow.dependent.GenericDependentResource;
import io.csviri.operator.resourceflow.dependent.GenericResourceDiscriminator;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;

@ControllerConfiguration
public class ResourceFlowReconciler implements Reconciler<ResourceFlow>, Cleaner<ResourceFlow> {

  private static final Logger log = LoggerFactory.getLogger(ResourceFlowReconciler.class);
  public static final String DEPENDENT_NAME_ANNOTATION_KEY = "io.csviri.operator.workflow/name";

  private final InformerRegister informerRegister = new InformerRegister();

  @Override
  public UpdateControl<ResourceFlow> reconcile(ResourceFlow primary,
      Context<ResourceFlow> context) {


    registerRelatedResourceInformers(context, primary);
    var actualWorkflow = buildWorkflowAndRegisterInformers(primary, context);
    actualWorkflow.reconcile(primary, context);
    cleanupRemovedResourcesFromWorkflow(context, primary);
    informerRegister.deRegisterInformerOnResourceFlowChange(context, primary);
    return UpdateControl.noUpdate();
  }

  private void registerRelatedResourceInformers(Context<ResourceFlow> context,
      ResourceFlow resourceFlow) {
    resourceFlow.getSpec().getRelatedResources().forEach(r -> {
      var gvk = new GroupVersionKind(r.getApiVersion(), r.getKind());
      informerRegister.registerInformerForRelatedResource(context, resourceFlow, r);
    });
  }

  @Override
  public DeleteControl cleanup(ResourceFlow primary, Context<ResourceFlow> context) {
    var actualWorkflow = buildWorkflowAndRegisterInformers(primary, context);

    // todo check if delete successfully executed / not postponed
    var result = actualWorkflow.cleanup(primary, context);

    actualWorkflow.getDependentResourcesByNameWithoutActivationCondition().forEach((n, dr) -> {
      var genericDependentResource = (GenericDependentResource) dr;
      informerRegister.deRegisterInformer(genericDependentResource.getGroupVersionKind(),
          primary, context);
    });
    informerRegister.deRegisterInformerForRelatedResources(primary, context);

    return DeleteControl.defaultDelete();
  }

  // todo test
  private void cleanupRemovedResourcesFromWorkflow(Context<ResourceFlow> context,
      ResourceFlow primary) {

    context.getSecondaryResources(GenericKubernetesResource.class).forEach(r -> {
      String dependentName = r.getMetadata().getAnnotations().get(DEPENDENT_NAME_ANNOTATION_KEY);
      // dependent name is null for related resources
      if (dependentName != null && primary.getSpec().getResources().stream()
          .filter(pr -> pr.getName().equals(dependentName)).findAny().isEmpty()) {
        try {
          log.debug("Deleting resource with name: {}", dependentName);
          context.getClient().resource(r).delete();
        } catch (KubernetesClientException e) {
          // can happen that already deleted, just in cache.
          log.warn("Error during deleting resource on workflow change", e);
        }
      }
    });
  }

  private io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow<ResourceFlow> buildWorkflowAndRegisterInformers(
      ResourceFlow primary, Context<ResourceFlow> context) {
    var builder = new WorkflowBuilder<ResourceFlow>();
    Set<String> leafDependentNames = Utils.leafResourceNames(primary);

    Map<String, GenericDependentResource> genericDependentResourceMap = new HashMap<>();
    primary.getSpec().getResources().forEach(spec -> createAndAddDependentToWorkflow(primary,
        context, spec, genericDependentResourceMap, builder,
        leafDependentNames.contains(spec.getName())));

    return builder.build();
  }

  private void createAndAddDependentToWorkflow(ResourceFlow primary, Context<ResourceFlow> context,
      DependentResourceSpec spec,
      Map<String, GenericDependentResource> genericDependentResourceMap,
      WorkflowBuilder<ResourceFlow> builder, boolean leafDependent) {


    var dr = createDependentResource(spec);
    var gvk = dr.getGroupVersionKind();

    dr.setResourceDiscriminator(new GenericResourceDiscriminator(dr.getGroupVersionKind(),
        Utils.getName(spec),
        Utils.getNamespace(spec).orElse(null)));

    var es = informerRegister.registerInformer(context, gvk, primary);
    dr.configureWith(es);

    builder.addDependentResource(dr);
    spec.getDependsOn().forEach(s -> builder.dependsOn(genericDependentResourceMap.get(s)));
    genericDependentResourceMap.put(spec.getName(), dr);

    Optional.ofNullable(spec.getReadyPostCondition())
        .ifPresent(c -> builder.withReadyPostcondition(toCondition(c)));
    Optional.ofNullable(spec.getCondition())
        .ifPresent(c -> builder.withReconcilePrecondition(toCondition(c)));
    Optional.ofNullable(spec.getDeletePostCondition())
        .ifPresent(c -> builder.withDeletePostcondition(toCondition(c)));
  }

  private static GenericDependentResource createDependentResource(DependentResourceSpec spec) {
    return spec.getResourceTemplate() != null
        ? new GenericDependentResource(spec.getResourceTemplate(), spec.getName())
        : new GenericDependentResource(spec.getResource(), spec.getName());
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

package io.csviri.operator.workflow.reconciler;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.workflow.Utils;
import io.csviri.operator.workflow.conditions.JavaScripCondition;
import io.csviri.operator.workflow.conditions.PodsReadyCondition;
import io.csviri.operator.workflow.customresource.workflow.DependentResourceSpec;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.csviri.operator.workflow.customresource.workflow.condition.ConditionSpec;
import io.csviri.operator.workflow.customresource.workflow.condition.JavaScriptConditionSpec;
import io.csviri.operator.workflow.customresource.workflow.condition.PodsReadyConditionSpec;
import io.csviri.operator.workflow.dependent.GenericDependentResource;
import io.csviri.operator.workflow.dependent.GenericResourceDiscriminator;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

import static io.csviri.operator.workflow.reconciler.WorkflowOperatorReconciler.*;

@ControllerConfiguration()
public class WorkflowReconciler implements Reconciler<Workflow>, Cleaner<Workflow> {

  private static final Logger log = LoggerFactory.getLogger(WorkflowReconciler.class);
  public static final String DEPENDENT_NAME_ANNOTATION_KEY = "io.csviri.operator.workflow/name";

  private final InformerRegister informerRegister = new InformerRegister();

  @Override
  public UpdateControl<Workflow> reconcile(Workflow primary,
      Context<Workflow> context) {

    registerWorkflowOperatorPrimaryInformerIfApplies(context, primary);
    // todo related resources cleanup informers
    // todo related resource can be already a child resource to other workflow test
    registerRelatedResourceInformers(context, primary);
    var actualWorkflow = buildWorkflowAndRegisterInformers(primary, context);
    actualWorkflow.reconcile(primary, context);
    cleanupRemovedResourcesFromWorkflow(context, primary);
    informerRegister.deRegisterInformerOnWorkflowChange(context, primary);
    return UpdateControl.noUpdate();
  }

  // todo deregister
  private void registerRelatedResourceInformers(Context<Workflow> context, Workflow workflow) {
    workflow.getSpec().getRelatedResources().forEach(r -> {
      var gvk = new GroupVersionKind(r.getApiVersion(), r.getKind());
      informerRegister.registerInformerForRelatedResource(context, workflow, gvk, r.getNamespace(),
          r.getResourceNames());
    });
  }

  @Override
  public DeleteControl cleanup(Workflow primary, Context<Workflow> context) {
    var actualWorkflow = buildWorkflowAndRegisterInformers(primary, context);

    actualWorkflow.getDependentResourcesByNameWithoutActivationCondition().forEach((n, dr) -> {
      var genericDependentResource = (GenericDependentResource) dr;
      informerRegister.deRegisterInformer(genericDependentResource.getGroupVersionKind(),
          primary, context);
    });

    var optionalGVK = gvkFromAnnotationForOperator(primary.getMetadata().getAnnotations());
    optionalGVK.ifPresent(gvk -> informerRegister.deRegisterInformer(gvk, primary, context));

    return DeleteControl.defaultDelete();
  }

  // todo test
  private void cleanupRemovedResourcesFromWorkflow(Context<Workflow> context,
      Workflow primary) {

    context.getSecondaryResources(GenericKubernetesResource.class).forEach(r -> {
      String dependentName = r.getMetadata().getAnnotations().get(DEPENDENT_NAME_ANNOTATION_KEY);
      if (primary.getSpec().getResources().stream()
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

  private io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow<Workflow> buildWorkflowAndRegisterInformers(
      Workflow primary, Context<Workflow> context) {
    var builder = new WorkflowBuilder<Workflow>();
    Set<String> leafDependentNames = Utils.leafResourceNames(primary);

    Map<String, GenericDependentResource> genericDependentResourceMap = new HashMap<>();
    primary.getSpec().getResources().forEach(spec -> createAndAddDependentToWorkflow(primary,
        context, spec, genericDependentResourceMap, builder,
        leafDependentNames.contains(spec.getName())));

    return builder.build();
  }

  private Optional<GroupVersionKind> registerWorkflowOperatorPrimaryInformerIfApplies(
      Context<Workflow> context,
      Workflow primary) {

    var optionalGVK = gvkFromAnnotationForOperator(primary.getMetadata().getAnnotations());
    if (optionalGVK.isEmpty()) {
      return Optional.empty();
    }

    GroupVersionKind gvk = optionalGVK.orElseThrow();

    informerRegister.registerInformer(context, primary, gvk, () -> new InformerEventSource<>(
        InformerConfiguration
            .from(gvk,
                context.eventSourceRetriever()
                    .eventSourceContextForDynamicRegistration())
            .withSecondaryToPrimaryMapper(
                resource -> Set.of(new ResourceID(resource.getMetadata().getName(),
                    resource.getMetadata().getNamespace())))
            .build(),
        context.eventSourceRetriever().eventSourceContextForDynamicRegistration()));

    return optionalGVK;
  }

  private void createAndAddDependentToWorkflow(Workflow primary, Context<Workflow> context,
      DependentResourceSpec spec,
      Map<String, GenericDependentResource> genericDependentResourceMap,
      WorkflowBuilder<Workflow> builder, boolean leafDependent) {


    var dr = createDependentResource(spec);
    var gvk = dr.getGroupVersionKind();

    dr.setResourceDiscriminator(new GenericResourceDiscriminator(dr.getGroupVersionKind(),
        Utils.getName(spec),
        Utils.getNamespace(spec).orElse(null)));

    informerRegister.registerInformer(context, primary, gvk,
        () -> dr.eventSource(context.eventSourceRetriever()
            .eventSourceContextForDynamicRegistration()).orElseThrow(),
        dr::configureWith);

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

  private Optional<GroupVersionKind> gvkFromAnnotationForOperator(Map<String, String> annotations) {
    if (!annotations.containsKey(WATCH_GROUP)) {
      return Optional.empty();
    }
    return Optional.of(new GroupVersionKind(annotations.get(WATCH_GROUP),
        annotations.get(WATCH_VERSION), annotations.get(WATCH_KIND)));
  }

}

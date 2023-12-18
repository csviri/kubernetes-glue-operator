package io.csviri.operator.workflow;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.csviri.operator.workflow.conditions.JavaScripCondition;
import io.csviri.operator.workflow.conditions.PodsReadyCondition;
import io.csviri.operator.workflow.customresource.workflow.DependentResourceSpec;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.csviri.operator.workflow.customresource.workflow.condition.ConditionSpec;
import io.csviri.operator.workflow.customresource.workflow.condition.JavaScriptConditionSpec;
import io.csviri.operator.workflow.customresource.workflow.condition.PodsReadyConditionSpec;
import io.csviri.operator.workflow.dependent.GenericDependentResource;
import io.csviri.operator.workflow.dependent.GenericResourceDiscriminator;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

import static io.csviri.operator.workflow.WorkflowOperatorReconciler.*;

@ControllerConfiguration()
public class WorkflowReconciler implements Reconciler<Workflow>, Cleaner<Workflow> {

  private final Map<String, Set<String>> registeredEventSourcesForGVK = new ConcurrentHashMap<>();

  // caching workflow in memory?

  public UpdateControl<Workflow> reconcile(Workflow primary,
      Context<Workflow> context) {

    var actualWorkflow = buildWorkflowAndRegisterEventSources(primary, context);
    actualWorkflow.reconcile(primary, context);

    return UpdateControl.noUpdate();
  }

  private io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow<Workflow> buildWorkflowAndRegisterEventSources(
      Workflow primary, Context<Workflow> context) {
    var builder = new WorkflowBuilder<Workflow>();

    Map<String, GenericDependentResource> genericDependentResourceMap = new HashMap<>();
    primary.getSpec().getResources().forEach(spec -> createAndAddDependentToWorkflow(primary,
        context, spec, genericDependentResourceMap, builder));

    // todo remove on cleanup
    addWorkflowOperatorPrimaryInformerIfApplies(context, primary);

    return builder.build();
  }

  private void addWorkflowOperatorPrimaryInformerIfApplies(Context<Workflow> context,
      Workflow primary) {
    var annotations = primary.getMetadata().getAnnotations();
    if (!annotations.containsKey(WATCH_GROUP)) {
      return;
    }
    GroupVersionKind gvk =
        new GroupVersionKind(annotations.get(WATCH_GROUP),
            annotations.get(WATCH_VERSION), annotations.get(WATCH_KIND));

    var ies = Utils.getInformerEventSource(context, gvk);
    if (ies.isEmpty()) {
      // todo race condition?
      context.eventSourceRetriever().dynamicallyRegisterEventSource(
          gvk.toString(), new InformerEventSource<>(
              InformerConfiguration
                  .from(gvk,
                      context.eventSourceRetriever().eventSourceContextForDynamicRegistration())
                  .withSecondaryToPrimaryMapper(
                      resource -> Set.of(new ResourceID(resource.getMetadata().getName(),
                          WORKFLOW_TARGET_NAMESPACE)))
                  .build(),
              context.eventSourceRetriever().eventSourceContextForDynamicRegistration()));
    }
    markEventSource(gvk, primary);

  }

  private void createAndAddDependentToWorkflow(Workflow primary, Context<Workflow> context,
      DependentResourceSpec spec,
      Map<String, GenericDependentResource> genericDependentResourceMap,
      WorkflowBuilder<Workflow> builder) {

    var dr = createDependentResource(spec);
    var gvk = dr.getGroupVersionKind();

    dr.setResourceDiscriminator(new GenericResourceDiscriminator(dr.getGroupVersionKind(),
        Utils.getName(spec),
        Utils.getNamespace(spec).orElse(null)));

    Utils.getInformerEventSource(context, gvk).ifPresentOrElse(dr::configureWith,
        () -> context.eventSourceRetriever().dynamicallyRegisterEventSource(
            gvk.toString(),
            dr.eventSource(
                context.eventSourceRetriever().eventSourceContextForDynamicRegistration())
                .orElseThrow()));
    markEventSource(gvk, primary);

    builder.addDependentResource(dr);
    spec.getDependsOn().forEach(s -> builder.dependsOn(genericDependentResourceMap.get(s)));
    genericDependentResourceMap.put(dependentName(spec), dr);

    Optional.ofNullable(spec.getReadyPostCondition())
        .ifPresent(c -> builder.withReadyPostcondition(toCondition(c)));
    Optional.ofNullable(spec.getCondition())
        .ifPresent(c -> builder.withReconcilePrecondition(toCondition(c)));
    Optional.ofNullable(spec.getDeletePostCondition())
        .ifPresent(c -> builder.withDeletePostcondition(toCondition(c)));
  }

  private static String dependentName(DependentResourceSpec spec) {
    return spec.getName() == null || spec.getName().isBlank()
        ? DependentResource.defaultNameFor((Class<? extends DependentResource>) spec.getClass())
        : spec.getName();
  }

  private static GenericDependentResource createDependentResource(DependentResourceSpec spec) {
    return spec.getResourceTemplate() != null
        ? new GenericDependentResource(spec.getResourceTemplate())
        : new GenericDependentResource(spec.getResource());
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

  @Override
  public DeleteControl cleanup(Workflow primary, Context<Workflow> context) {
    // todo handle race condition between registration and deregistration
    var actualWorkflow = buildWorkflowAndRegisterEventSources(primary, context);
    actualWorkflow.getDependentResourcesByNameWithoutActivationCondition().forEach((n, dr) -> {
      var genericDependentResource = (GenericDependentResource) dr;
      var lastForGVK = unmarkEventSource(genericDependentResource, primary);
      if (lastForGVK) {
        context.eventSourceRetriever().dynamicallyDeRegisterEventSource(genericDependentResource
            .getGroupVersionKind().toString());
      }
    });
    return DeleteControl.defaultDelete();
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

  private synchronized boolean unmarkEventSource(GenericDependentResource genericDependentResource,
      Workflow workflow) {
    var key = genericDependentResource.getGroupVersionKind().toString();
    var es = registeredEventSourcesForGVK.get(key);
    es.remove(workflowId(workflow));
    return es.isEmpty();
  }

  private String workflowId(Workflow workflow) {
    return workflow.getMetadata().getName() + "#" + workflow.getMetadata().getNamespace();
  }

}

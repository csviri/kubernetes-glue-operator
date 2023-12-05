package io.csviri.operator.workflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.csviri.operator.workflow.conditions.PodsReadyCondition;
import io.csviri.operator.workflow.customresource.workflow.ConditionSpec;
import io.csviri.operator.workflow.customresource.workflow.PodsReadyConditionSpec;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

@ControllerConfiguration()
public class WorkflowReconciler implements Reconciler<Workflow>, Cleaner<Workflow> {

  // GVK -> Workflows
  private final Map<String, Set<String>> registeredEventSources = new ConcurrentHashMap<>();

  // todo workflow resource removal? caching workflow in memory

  public UpdateControl<Workflow> reconcile(Workflow primary,
      Context<Workflow> context) {

    var actualWorkflow = buildWorkflow(primary);

    var esc = new EventSourceContext<>(null, // todo fix this null?
        context.getControllerConfiguration(),
        context.getClient());

    actualWorkflow.getDependentResourcesByNameWithoutActivationCondition().forEach((n, dr) -> {
      GenericDependentResource genericDependentResource = (GenericDependentResource) dr;
      context.eventSourceRetriever().dynamicallyRegisterEventSource(
          genericDependentResource.getGroupVersionKind().toString(),
          (EventSource) dr.eventSource(esc).orElseThrow());
      markEventSource(genericDependentResource, primary);
    });

    actualWorkflow.reconcile(primary, context);

    return UpdateControl.noUpdate();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow<Workflow> buildWorkflow(
      Workflow primary) {
    var builder = new WorkflowBuilder<Workflow>();
    Map<String, GenericDependentResource> genericDependentResourceMap = new HashMap<>();

    primary.getSpec().getResources().forEach(spec -> {
      var dr = new GenericDependentResource(spec.getResource());
      String name = spec.getName() == null || spec.getName().isBlank()
          ? DependentResource.defaultNameFor((Class<? extends DependentResource>) spec.getClass())
          : spec.getName();

      genericDependentResourceMap.put(name, dr);
      builder.addDependentResource(dr);
      // todo descriptive error handling
      spec.getDependsOn().forEach(s -> builder.dependsOn(genericDependentResourceMap.get(s)));

      var readyPostConditionDefinition = spec.getReadyPostCondition();
      if (readyPostConditionDefinition != null) {
        builder.withReadyPostcondition(toCondition(readyPostConditionDefinition));
      }
      var condition = spec.getCondition();
      if (condition != null) {
        builder.withReconcilePrecondition(toCondition(condition));
      }
      var deletePostCondition = spec.getDeletePostCondition();
      if (condition != null) {
        builder.withDeletePostcondition(toCondition(deletePostCondition));
      }
    });
    return builder.build();
  }

  private Condition toCondition(ConditionSpec readyPostConditionDefinition) {
    if (readyPostConditionDefinition instanceof PodsReadyConditionSpec) {
      PodsReadyConditionSpec conditionSpec = (PodsReadyConditionSpec) readyPostConditionDefinition;
      return new PodsReadyCondition(conditionSpec.isNegated());
    }
    throw new IllegalStateException("Unknown condition: " + readyPostConditionDefinition);
  }

  @Override
  public DeleteControl cleanup(Workflow primary, Context<Workflow> context) {
    // todo handle race condition between registration and deregistration
    var actualWorkflow = buildWorkflow(primary);
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

  private synchronized void markEventSource(GenericDependentResource genericDependentResource,
      Workflow workflow) {
    var key = genericDependentResource.getGroupVersionKind().toString();
    registeredEventSources.merge(key, new HashSet<>(Set.of(workflowId(workflow))), (s1, s2) -> {
      s1.addAll(s2);
      return s1;
    });
  }

  private synchronized boolean unmarkEventSource(GenericDependentResource genericDependentResource,
      Workflow workflow) {
    var key = genericDependentResource.getGroupVersionKind().toString();
    var es = registeredEventSources.get(key);
    es.remove(workflowId(workflow));
    return es.isEmpty();
  }

  private String workflowId(Workflow workflow) {
    return workflow.getMetadata().getName() + "#" + workflow.getMetadata().getNamespace();
  }

}

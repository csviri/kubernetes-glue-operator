package io.csviri.operator.workflow;

import java.util.Map;
import java.util.Set;

import io.csviri.operator.workflow.customresource.operator.WorkflowOperator;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

@ControllerConfiguration
public class WorkflowOperatorReconciler
    implements Reconciler<WorkflowOperator>, EventSourceInitializer<WorkflowOperator> {

  public static final String WORKFLOW_LABEL_KEY = "foroperator";
  public static final String WORKFLOW_LABEL_VALUE = "true";

  private InformerEventSource<Workflow, WorkflowOperator> workflowEventSource;

  @Override
  public UpdateControl<WorkflowOperator> reconcile(WorkflowOperator workflowOperator,
      Context<WorkflowOperator> context) {

    var targetCREventSource = getOrRegisterEventSource(workflowOperator, context);
    targetCREventSource.list().forEach(cr -> {
      // todo manage workflows for CRs
    });

    return UpdateControl.noUpdate();
  }

  private InformerEventSource<GenericKubernetesResource, WorkflowOperator> getOrRegisterEventSource(
      WorkflowOperator workflowOperator, Context<WorkflowOperator> context) {
    var spec = workflowOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getGroup(), spec.getVersion(), spec.getKind());
    InformerEventSource<GenericKubernetesResource, WorkflowOperator> es = null;
    try {
      es = (InformerEventSource<GenericKubernetesResource, WorkflowOperator>) context
          .eventSourceRetriever()
          .getResourceEventSourceFor(GenericKubernetesResource.class, gvk.toString());
    } catch (IllegalArgumentException e) {

      es = new InformerEventSource<>(InformerConfiguration.from(gvk,
          context.eventSourceRetriever().eventSourceContexForDynamicRegistration())
          .withSecondaryToPrimaryMapper(
              resource -> Set.of(ResourceID.fromResource(workflowOperator)))
          .build(),

          context.eventSourceRetriever().eventSourceContexForDynamicRegistration());
      context.eventSourceRetriever().dynamicallyRegisterEventSource(gvk.toString(), es);
    }
    return es;
  }



  @Override
  public Map<String, EventSource> prepareEventSources(
      EventSourceContext<WorkflowOperator> eventSourceContext) {
    workflowEventSource = new InformerEventSource<>(
        InformerConfiguration.from(Workflow.class, eventSourceContext)
            .withLabelSelector(WORKFLOW_LABEL_KEY + "=" + WORKFLOW_LABEL_VALUE)
            .build(),
        eventSourceContext);
    return EventSourceInitializer.nameEventSources(workflowEventSource);
  }
}

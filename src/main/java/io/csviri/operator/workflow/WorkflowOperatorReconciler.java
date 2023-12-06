package io.csviri.operator.workflow;

import java.util.Map;

import io.csviri.operator.workflow.customresource.operator.WorkflowOperator;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

@ControllerConfiguration
public class WorkflowOperatorReconciler
    implements Reconciler<WorkflowOperator>, EventSourceInitializer<WorkflowOperator> {

  @Override
  public UpdateControl<WorkflowOperator> reconcile(WorkflowOperator workflowOperator,
      Context<WorkflowOperator> context) {

    return UpdateControl.noUpdate();
  }


  @Override
  public Map<String, EventSource> prepareEventSources(
      EventSourceContext<WorkflowOperator> eventSourceContext) {
    var workflowEventSource = new InformerEventSource<>(
        InformerConfiguration.from(Workflow.class, eventSourceContext).build(), eventSourceContext);
    return EventSourceInitializer.nameEventSources(workflowEventSource);
  }
}

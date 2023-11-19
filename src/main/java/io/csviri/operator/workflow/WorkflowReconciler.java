package io.csviri.operator.workflow;

import io.csviri.operator.workflow.customresource.WorkflowCustomResource;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.IndexerResourceCache;

@ControllerConfiguration()
public class WorkflowReconciler implements Reconciler<WorkflowCustomResource> {

    public UpdateControl<WorkflowCustomResource> reconcile(WorkflowCustomResource primary,
                                                           Context<WorkflowCustomResource> context) {

        var builder = new WorkflowBuilder<WorkflowCustomResource>();
        addDependents(primary, builder);
        var workflow = builder.build();

        var esc = new EventSourceContext<WorkflowCustomResource>(null,
                context.getControllerConfiguration(),
                context.getClient());

        workflow.getDependentResourcesByNameWithoutActivationCondition().forEach((n,dr)->{
            // todo uniform name
            context.eventSourceRetriever().dynamicallyRegisterEventSource(n, (EventSource) dr.eventSource(esc).orElseThrow());
        });

        workflow.reconcile(primary,context);

        return UpdateControl.noUpdate();
    }

    private void addDependents(WorkflowCustomResource primary, WorkflowBuilder<WorkflowCustomResource> builder) {
        primary.getSpec().getResources().forEach(dr-> {
            var genericDR = new GenericDependentResource(dr.getResource());
            builder.addDependentResource(genericDR);
        });
    }
}

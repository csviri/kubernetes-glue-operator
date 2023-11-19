package io.csviri.operator.workflow;

import io.csviri.operator.workflow.customresource.Workflow;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

@ControllerConfiguration()
public class WorkflowReconciler implements Reconciler<Workflow> {

    public UpdateControl<Workflow> reconcile(Workflow primary,
                                             Context<Workflow> context) {

        var builder = new WorkflowBuilder<Workflow>();
        addDependents(primary, builder);
        var workflow = builder.build();

        var esc = new EventSourceContext<>(null, // todo fix this null?
                context.getControllerConfiguration(),
                context.getClient());

        workflow.getDependentResourcesByNameWithoutActivationCondition().forEach((n,dr)->{
            // todo unique name
            context.eventSourceRetriever().dynamicallyRegisterEventSource(n, (EventSource) dr.eventSource(esc).orElseThrow());
        });

        workflow.reconcile(primary,context);

        return UpdateControl.noUpdate();
    }

    private void addDependents(Workflow primary, WorkflowBuilder<Workflow> builder) {
        primary.getSpec().getResources().forEach(dr-> {
            var genericDR = new GenericDependentResource(dr.getResource().getClass(), dr.getResource());
            builder.addDependentResource(genericDR);
        });
    }
}

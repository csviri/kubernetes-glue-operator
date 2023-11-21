package io.csviri.operator.workflow;

import io.csviri.operator.workflow.conditions.PodsReadyCondition;
import io.csviri.operator.workflow.customresource.ConditionSpec;
import io.csviri.operator.workflow.customresource.PodsReadyConditionSpec;
import io.csviri.operator.workflow.customresource.Workflow;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

import java.util.HashMap;
import java.util.Map;

@ControllerConfiguration()
public class WorkflowReconciler implements Reconciler<Workflow> {

    public UpdateControl<Workflow> reconcile(Workflow primary,
                                             Context<Workflow> context) {

        var workflow = buildWorkflow(primary);


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

    @SuppressWarnings({"rawtypes","unchecked"})
    private io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow<Workflow> buildWorkflow(Workflow primary) {
        var builder = new WorkflowBuilder<Workflow>();
        Map<String, GenericDependentResource> genericDependentResourceMap = new HashMap<>();

        primary.getSpec().getResources().forEach(spec-> {
            var dr = new GenericDependentResource(spec.getResource().getClass(), spec.getResource());
            String name = spec.getName() == null || spec.getName().isBlank() ?
                    DependentResource.defaultNameFor((Class<? extends DependentResource>) spec.getClass()) : spec.getName();

            genericDependentResourceMap.put(name,dr);
            builder.addDependentResource(dr);
            // todo descriptive error handling
            spec.getDependsOn().forEach(s->builder.dependsOn(genericDependentResourceMap.get(s)));
            var readyPostConditionDefinition = spec.getReadyPostCondition();
            if (readyPostConditionDefinition != null) {
                builder.withReadyPostcondition(toCondition(readyPostConditionDefinition));
            }

        });
        return builder.build();
    }

    private Condition toCondition(ConditionSpec
                                          readyPostConditionDefinition) {
        if (readyPostConditionDefinition instanceof PodsReadyConditionSpec) {
            PodsReadyConditionSpec conditionSpec = (PodsReadyConditionSpec) readyPostConditionDefinition;
            return new PodsReadyCondition(conditionSpec.isNegated());
        }
        throw new IllegalStateException("Unknown condition: "+ readyPostConditionDefinition);
    }

    // todo cleanup, deregister event sources
}

package io.csviri.operator.workflow;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

import java.util.Map;
import java.util.Optional;

@ControllerConfiguration(dependents = {@Dependent(type = ConfigMapDependentResource.class)})
public class WorkflowOperatorReconciler implements Reconciler<WorkflowOperatorCustomResource> {

    public UpdateControl<WorkflowOperatorCustomResource> reconcile(WorkflowOperatorCustomResource primary,
                                                     Context<WorkflowOperatorCustomResource> context) {

        return UpdateControl.noUpdate();
    }
}

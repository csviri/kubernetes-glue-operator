package io.csviri.operator.workflow;

import io.csviri.operator.workflow.customresource.WorkflowCustomResource;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;

@ControllerConfiguration()
public class WorkflowReconciler implements Reconciler<WorkflowCustomResource> {

    public UpdateControl<WorkflowCustomResource> reconcile(WorkflowCustomResource primary,
                                                           Context<WorkflowCustomResource> context) {




        return UpdateControl.noUpdate();
    }
}

package io.csviri.operator.workflow;

import io.csviri.operator.workflow.customresource.WorkflowCustomResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

public class GenericDependentResource extends CRUDKubernetesDependentResource<GenericKubernetesResource,
        WorkflowCustomResource>  {

    private GenericKubernetesResource desired;

    public GenericDependentResource(GenericKubernetesResource desired) {
        super(GenericKubernetesResource.class);
        this.desired = desired;
    }

    @Override
    protected GenericKubernetesResource desired(WorkflowCustomResource primary,
                                                Context<WorkflowCustomResource> context) {

        // todo set name if not set, same as primary?
        // todo should this be closed at all?
        if (desired.getMetadata().getNamespace() == null) {
            var cloned = context.getControllerConfiguration().getConfigurationService().getResourceCloner().clone(desired);
            cloned.getMetadata().setNamespace(primary.getMetadata().getNamespace());
            return cloned;
        }

        return desired;
    }
}

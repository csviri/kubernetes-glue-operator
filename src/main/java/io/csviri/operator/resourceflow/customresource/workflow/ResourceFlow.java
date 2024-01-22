package io.csviri.operator.resourceflow.customresource.workflow;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("io.csviri.operator.resourceflow")
@Version("v1beta1")
@ShortNames("rf")
public class ResourceFlow extends CustomResource<WorkflowSpec, WorkflowStatus>
    implements Namespaced {
}

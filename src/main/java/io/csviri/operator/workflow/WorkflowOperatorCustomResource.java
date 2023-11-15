package io.csviri.operator.workflow;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("io.csviri.operator.workflow")
@Version("v1")
public class WorkflowOperatorCustomResource extends CustomResource<WorkflowOperatorSpec,WorkflowOperatorStatus> implements Namespaced {
}

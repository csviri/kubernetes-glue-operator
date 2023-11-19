package io.csviri.operator.workflow.customresource;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("io.csviri.operator.workflow")
@Version("v1")
@ShortNames("wf")
public class Workflow extends CustomResource<WorkflowSpec, WorkflowStatus> implements Namespaced {
}

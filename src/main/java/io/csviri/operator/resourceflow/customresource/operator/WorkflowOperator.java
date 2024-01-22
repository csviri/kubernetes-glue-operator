package io.csviri.operator.resourceflow.customresource.operator;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("io.csviri.operator.resourceflow")
@Version("v1beta1")
@ShortNames("wo")
public class WorkflowOperator extends CustomResource<WorkflowOperatorSpec, WorkflowOperatorStatus>
    implements Namespaced {
}

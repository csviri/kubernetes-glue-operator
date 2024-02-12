package io.csviri.operator.resourceglue.customresource.operator;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("io.csviri.operator.resourceglue")
@Version("v1beta1")
@ShortNames("rgo")
public class GlueOperator
    extends CustomResource<ResourceGlueOperatorSpec, ResourceFlowOperatorStatus>
    implements Namespaced {
}

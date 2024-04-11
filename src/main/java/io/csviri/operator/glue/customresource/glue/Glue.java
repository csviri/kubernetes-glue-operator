package io.csviri.operator.glue.customresource.glue;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("io.csviri.operator.resourceglue")
@Version("v1beta1")
@ShortNames("rg")
public class Glue extends CustomResource<ResourceGlueSpec, ResourceGlueStatus>
    implements Namespaced {
}

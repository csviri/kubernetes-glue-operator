package io.csviri.operator.workflow.customresource;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("io.csviri.operator.workflow")
@Version("v1beta1")
@ShortNames("tcr")
public class TestCustomResource extends CustomResource<TestCustomResourceSpec, Void> {

}

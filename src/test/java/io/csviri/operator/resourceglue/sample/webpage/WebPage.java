package io.csviri.operator.resourceglue.sample.webpage;

import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;

@Version(value = "v1", storage = true, served = true)
@Group("resourceglueoperator.sample")
@Singular("webpage")
@Plural("webpages")
@javax.annotation.processing.Generated("io.fabric8.java.generator.CRGeneratorRunner")
public class WebPage extends io.fabric8.kubernetes.client.CustomResource<WebPageSpec, WebPageStatus>
    implements io.fabric8.kubernetes.api.model.Namespaced {
}

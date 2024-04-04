package io.csviri.operator.resourceglue;

import org.junit.jupiter.api.BeforeEach;

import io.csviri.operator.resourceglue.customresource.glue.Glue;
import io.csviri.operator.resourceglue.customresource.operator.GlueOperator;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

public class WebPageE2E {

  private KubernetesClient client = new KubernetesClientBuilder().build();

  @BeforeEach
  void applyCRDs() {
    TestUtils.applyCrd(client, Glue.class, GlueOperator.class);
    
  }


}

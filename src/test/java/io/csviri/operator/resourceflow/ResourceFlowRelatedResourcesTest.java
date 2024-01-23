package io.csviri.operator.resourceflow;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.resourceflow.customresource.ClusterScopeTestCustomResource;
import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlow;
import io.csviri.operator.resourceflow.reconciler.ResourceFlowReconciler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ResourceFlowRelatedResourcesTest {

  private static final String BASE64_VALUE =
      Base64.getEncoder().encodeToString("val1".getBytes(StandardCharsets.UTF_8));

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder()
          .withReconciler(new ResourceFlowReconciler())
          .withAdditionalCustomResourceDefinition(ClusterScopeTestCustomResource.class)
          .build();

  @Test
  void simpleRelatedResourceUsage() {
    extension.create(secret());
    ResourceFlow resourceFlow =
        TestUtils.loadResoureFlow("/ResourceFlowRelatesResourceSimple.yaml");
    extension.create(resourceFlow);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      assertThat(cm1).isNotNull();
      assertThat(cm1.getData()).containsEntry("key", BASE64_VALUE);

      var cm2 = extension.get(ConfigMap.class, "cm2");
      assertThat(cm2).isNotNull();
    });
  }

  @Disabled
  @Test
  void multipleRelatedResourceUsage() {
    // todo
  }

  Secret secret() {
    return new SecretBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName("secret1")
            .build())
        .withData(Map.of("key", BASE64_VALUE))
        .build();

  }

}

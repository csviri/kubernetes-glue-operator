package io.csviri.operator.resourceflow;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

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

  private static final String BASE64_VALUE_1 =
      Base64.getEncoder().encodeToString("val1".getBytes(StandardCharsets.UTF_8));
  private static final String BASE64_VALUE_2 =
      Base64.getEncoder().encodeToString("val2".getBytes(StandardCharsets.UTF_8));

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
        TestUtils.loadResoureFlow("/resourceflow/RelatedResourceSimple.yaml");

    extension.create(resourceFlow);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      assertThat(cm1).isNotNull();
      assertThat(cm1.getData()).containsEntry("key", BASE64_VALUE_1);

      var cm2 = extension.get(ConfigMap.class, "cm2");
      assertThat(cm2).isNotNull();
    });

    extension.delete(resourceFlow);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      var cm2 = extension.get(ConfigMap.class, "cm2");

      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void multipleResourceNamesInRelated() {
    extension.create(secret("test-secret1", BASE64_VALUE_1));
    extension.create(secret("test-secret2", BASE64_VALUE_2));

    ResourceFlow resourceFlow =
        extension.create(TestUtils.loadResoureFlow("/resourceflow/MultiNameRelatedResource.yaml"));

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      assertThat(cm1).isNotNull();
      assertThat(cm1.getData()).containsEntry("key1", BASE64_VALUE_1);
      assertThat(cm1.getData()).containsEntry("key2", BASE64_VALUE_2);
    });

    extension.delete(resourceFlow);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      assertThat(cm1).isNull();
    });
  }

  void managedAndRelatedResourceOfSameType() {

  }

  Secret secret() {
    return secret("test-secret1", BASE64_VALUE_1);
  }

  Secret secret(String name, String val) {
    return new SecretBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName(name)
            .build())
        .withData(Map.of("key", val))
        .build();

  }

}

package io.csviri.operator.resourceglue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.resourceglue.customresource.ClusterScopeTestCustomResource;
import io.csviri.operator.resourceglue.customresource.glue.Glue;
import io.csviri.operator.resourceglue.reconciler.glue.GlueReconciler;
import io.fabric8.kubernetes.api.model.*;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class GlueRelatedResourcesTest {

  private static final String BASE64_VALUE_1 =
      Base64.getEncoder().encodeToString("val1".getBytes(StandardCharsets.UTF_8));
  private static final String BASE64_VALUE_2 =
      Base64.getEncoder().encodeToString("val2".getBytes(StandardCharsets.UTF_8));
  public static final String CONFIG_MAP_VALUE_1 = "val1";
  public static final String CONFIG_MAP_VALUE_2 = "val2";

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder()
          .withReconciler(new GlueReconciler())
          .withAdditionalCustomResourceDefinition(ClusterScopeTestCustomResource.class)
          .build();

  @Test
  void simpleRelatedResourceUsage() {
    extension.create(secret());
    Glue glue =
        TestUtils.loadResoureFlow("/resourceglue/RelatedResourceSimple.yaml");

    extension.create(glue);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      assertThat(cm1).isNotNull();
      assertThat(cm1.getData()).containsEntry("key", BASE64_VALUE_1);

      var cm2 = extension.get(ConfigMap.class, "cm2");
      assertThat(cm2).isNotNull();
    });

    extension.delete(glue);

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

    Glue glue =
        extension.create(TestUtils.loadResoureFlow("/resourceglue/MultiNameRelatedResource.yaml"));

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      assertThat(cm1).isNotNull();
      assertThat(cm1.getData()).containsEntry("key1", BASE64_VALUE_1);
      assertThat(cm1.getData()).containsEntry("key2", BASE64_VALUE_2);
    });

    extension.delete(glue);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      assertThat(cm1).isNull();
    });
  }

  @Test
  void managedAndRelatedResourceOfSameTypeAndTriggering() {
    var relatedConfigMap = extension.create(configMap());
    Glue glue =
        extension.create(TestUtils.loadResoureFlow("/resourceglue/RelatesResourceSameType.yaml"));

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      assertThat(cm1).isNotNull();
      assertThat(cm1.getData()).containsEntry("key", CONFIG_MAP_VALUE_1);
    });

    // changes on the managed config map reverted
    var cm = extension.get(ConfigMap.class, "cm1");
    cm.getData().put("key", CONFIG_MAP_VALUE_2);
    extension.replace(cm);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      assertThat(cm1.getData()).containsEntry("key", CONFIG_MAP_VALUE_1);
    });

    relatedConfigMap.getData().put("key", CONFIG_MAP_VALUE_2);
    extension.replace(relatedConfigMap);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      assertThat(cm1.getData()).containsEntry("key", CONFIG_MAP_VALUE_2);
    });

    extension.delete(glue);
    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "cm1");
      assertThat(cm1).isNull();
    });
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

  ConfigMap configMap() {
    return new ConfigMapBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName("related-cm1")
            .build())
        .withData(Map.of("key", CONFIG_MAP_VALUE_1))
        .build();
  }

}

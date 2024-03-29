package io.csviri.operator.resourceglue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.csviri.operator.resourceglue.customresource.glue.DependentResourceSpec;
import io.csviri.operator.resourceglue.customresource.glue.Glue;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
class GlueTest extends TestBase {

  public static final String CHANGED_VALUE = "changed_value";

  @SuppressWarnings("unchecked")
  @Test
  void simpleTemplating() {
    Glue glue =
        TestUtils.loadResoureFlow("/resourceglue/Templating.yaml");
    glue = create(glue);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "templconfigmap1");
      var cm2 = get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();

      assertThat(cm2.getData().get("valueFromCM1")).isEqualTo("value1");
    });

    ((Map<String, String>) glue.getSpec().getResources().get(0).getResource()
        .getAdditionalProperties().get("data"))
        .put("key", CHANGED_VALUE);

    update(glue);

    await().untilAsserted(() -> {
      var cm2 = get(ConfigMap.class, "templconfigmap2");
      assertThat(cm2.getData().get("valueFromCM1")).isEqualTo(CHANGED_VALUE);
    });

    delete(glue);

    await().timeout(Duration.ofSeconds(30)).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "templconfigmap1");
      var cm2 = get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @SuppressWarnings("unchecked")
  @Test
  void javaScriptCondition() {
    Glue glue =
        TestUtils.loadResoureFlow("/resourceglue/TwoResourcesAndCondition.yaml");
    create(glue);

    await().pollDelay(Duration.ofMillis(150)).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var cm2 = get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNull();
    });

    Map<String, String> map = (Map<String, String>) glue.getSpec().getResources()
        .get(0).getResource().getAdditionalProperties().get("data");
    map.put("createOther", "true");

    update(glue);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var cm2 = get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();
    });

    delete(glue);

    await().timeout(Duration.ofSeconds(GC_TIMEOUT_SEC)).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var cm2 = get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void stringTemplate() {
    Glue glue = create(TestUtils.loadResoureFlow("/resourceglue/ResourceTemplate.yaml"));

    await().timeout(Duration.ofSeconds(120)).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "templconfigmap1");
      var cm2 = get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();

      assertThat(cm2.getData().get("valueFromCM1")).isEqualTo("value1");
    });

    delete(glue);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "templconfigmap1");
      var cm2 = get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void simpleConcurrencyTest() {
    // todo set 10
    int num = 1;
    List<Glue> glueList = testWorkflowList(num);

    glueList.forEach(this::create);

    await().untilAsserted(() -> IntStream.range(0, num).forEach(index -> {

      var w = get(Glue.class, "testglue" + index);
      assertThat(w).isNotNull();
      var cm1 = get(ConfigMap.class, "testglue" + index + "-1");
      var cm2 = get(ConfigMap.class, "testglue" + index + "-2");

      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();
    }));

    glueList.forEach(this::delete);
    await().timeout(Duration.ofMinutes(5)).untilAsserted(() -> IntStream.range(0, num).forEach(index -> {
      var w = get(Glue.class, "testglue" + index);
      assertThat(w).isNull();
    }));
  }

  @Test
  void changingWorkflow() {
    Glue glue = create(TestUtils.loadResoureFlow("/resourceglue/ChanginResources.yaml"));

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var cm2 = get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();
    });

    glue.getSpec().getResources().remove(1);
    glue.getSpec().getResources().add(new DependentResourceSpec()
        .setName("secret")
        .setResource(TestUtils.load("/Secret.yaml")));

    update(glue);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var cm2 = get(ConfigMap.class, "configmap2");
      var s = get(Secret.class, "secret1");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNull();
      assertThat(s).isNotNull();
    });

    glue.getMetadata().setResourceVersion(null);
    delete(glue);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var s = get(Secret.class, "secret1");
      assertThat(cm1).isNull();
      assertThat(s).isNull();
    });
  }
  //
  // @Disabled("Not supported in current version")
  // @Test
  // void childInDifferentNamespaceAsPrimary() {
  // Glue w = extension
  // .create(TestUtils.loadResoureFlow("/resourceglue/ResourceInDifferentNamespace.yaml"));
  //
  // await().untilAsserted(() -> {
  // var cmDifferentNS = extension.getKubernetesClient().configMaps().inNamespace("default")
  // .withName("configmap1");
  // var cm2 = extension.get(ConfigMap.class, "configmap2");
  //
  // assertThat(cmDifferentNS).isNotNull();
  // assertThat(cm2).isNotNull();
  // });
  //
  // extension.delete(w);
  //
  // await().untilAsserted(() -> {
  // var cmDifferentNS = extension.getKubernetesClient().configMaps().inNamespace("default")
  // .withName("configmap1");
  // var cm2 = extension.get(ConfigMap.class, "configmap2");
  //
  // assertThat(cmDifferentNS).isNull();
  // assertThat(cm2).isNull();
  // });
  //
  // }

  private List<Glue> testWorkflowList(int num) {
    List<Glue> res = new ArrayList<>();
    IntStream.range(0, num).forEach(index -> {
      Glue w =
          TestUtils.loadResoureFlow("/resourceglue/TemplateForConcurrency.yaml");
      w.getMetadata().setName(w.getMetadata().getName() + index);
      res.add(w);
    });
    return res;
  }

}

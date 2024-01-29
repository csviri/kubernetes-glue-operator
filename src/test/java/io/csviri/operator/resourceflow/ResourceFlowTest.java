package io.csviri.operator.resourceflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.resourceflow.customresource.ClusterScopeTestCustomResource;
import io.csviri.operator.resourceflow.customresource.resourceflow.DependentResourceSpec;
import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlow;
import io.csviri.operator.resourceflow.reconciler.flow.ResourceFlowReconciler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ResourceFlowTest {

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder()
          .withReconciler(new ResourceFlowReconciler())
          .withAdditionalCustomResourceDefinition(ClusterScopeTestCustomResource.class)
          .build();

  @SuppressWarnings("unchecked")
  @Test
  void javaScriptCondition() {
    ResourceFlow resourceFlow =
        TestUtils.loadResoureFlow("/resourceflow/TwoResourcesAndCondition.yaml");
    resourceFlow = extension.create(resourceFlow);

    await().pollDelay(Duration.ofMillis(150)).untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap1");
      var cm2 = extension.get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNull();
    });

    Map<String, String> map = (Map<String, String>) resourceFlow.getSpec().getResources()
        .get(0).getResource().getAdditionalProperties().get("data");
    map.put("createOther", "true");
    extension.replace(resourceFlow);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap1");
      var cm2 = extension.get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();
    });

    extension.delete(resourceFlow);
    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap1");
      var cm2 = extension.get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void templatingObject() {
    ResourceFlow resourceFlow =
        TestUtils.loadResoureFlow("/resourceflow/Templating.yaml");
    resourceFlow = extension.create(resourceFlow);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "templconfigmap1");
      var cm2 = extension.get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();

      assertThat(cm2.getData().get("valueFromCM1")).isEqualTo("value1");
    });

    extension.delete(resourceFlow);
    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "templconfigmap1");
      var cm2 = extension.get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void stringTemplate() {
    ResourceFlow resourceFlow =
        TestUtils.loadResoureFlow("/resourceflow/ResourceTemplate.yaml");

    resourceFlow = extension.create(resourceFlow);

    await().timeout(Duration.ofSeconds(120)).untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "templconfigmap1");
      var cm2 = extension.get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();

      assertThat(cm2.getData().get("valueFromCM1")).isEqualTo("value1");
    });

    extension.delete(resourceFlow);
    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "templconfigmap1");
      var cm2 = extension.get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void simpleConcurrencyTest() {
    int num = 10;
    List<ResourceFlow> resourceFlowList = testWorkflowList(num);

    resourceFlowList.forEach(w -> {
      extension.create(w);
    });

    await().untilAsserted(() -> IntStream.range(0, num).forEach(index -> {
      var w = extension.get(ResourceFlow.class, "testworkflow" + index);
      assertThat(w).isNotNull();
      var cm1 = extension.get(ConfigMap.class, "testworkflow" + index + "-1");
      var cm2 = extension.get(ConfigMap.class, "testworkflow" + index + "-2");

      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();
    }));

    resourceFlowList.forEach(w -> {
      extension.delete(w);
    });
    await().untilAsserted(() -> IntStream.range(0, num).forEach(index -> {
      var w = extension.get(ResourceFlow.class, "testworkflow" + index);
      assertThat(w).isNull();
    }));
  }

  @Test
  void changingWorkflow() {
    ResourceFlow w =
        extension.create(TestUtils.loadResoureFlow("/resourceflow/ChanginResources.yaml"));

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap1");
      var cm2 = extension.get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();
    });

    w.getSpec().getResources().remove(1);
    w.getSpec().getResources().add(new DependentResourceSpec()
        .setName("secret")
        .setResource(TestUtils.load("/Secret.yaml")));
    w = extension.replace(w);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap1");
      var cm2 = extension.get(ConfigMap.class, "configmap2");
      var s = extension.get(Secret.class, "secret1");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNull();
      assertThat(s).isNotNull();
    });

    extension.delete(w);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap1");
      var s = extension.get(Secret.class, "secret1");
      assertThat(cm1).isNull();
      assertThat(s).isNull();
    });
  }

  private List<ResourceFlow> testWorkflowList(int num) {
    List<ResourceFlow> res = new ArrayList<>();
    IntStream.range(0, num).forEach(index -> {
      ResourceFlow w =
          TestUtils.loadResoureFlow("/resourceflow/TemplateForConcurrency.yaml");
      w.getMetadata().setName(w.getMetadata().getName() + index);
      res.add(w);
    });
    return res;
  }

}

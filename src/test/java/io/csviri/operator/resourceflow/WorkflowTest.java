package io.csviri.operator.resourceflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.resourceflow.customresource.ClusterScopeTestCustomResource;
import io.csviri.operator.resourceflow.customresource.workflow.DependentResourceSpec;
import io.csviri.operator.resourceflow.customresource.workflow.ResourceFlow;
import io.csviri.operator.resourceflow.reconciler.WorkflowReconciler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class WorkflowTest {

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder()
          .withReconciler(new WorkflowReconciler())
          .withAdditionalCustomResourceDefinition(ClusterScopeTestCustomResource.class)
          .build();

  @SuppressWarnings("unchecked")
  @Test
  void javaScriptCondition() {
    ResourceFlow resourceFlow = TestUtils.loadWorkflow("/Workflow2ResourceAndCondition.yaml");
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
    ResourceFlow resourceFlow = TestUtils.loadWorkflow("/WorkflowTemplating.yaml");
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
    ResourceFlow resourceFlow = TestUtils.loadWorkflow("/WorkflowWithResourceTemplate.yaml");

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
  void handlingClusterScopeDependents() {

    final var clusterScopedResourceName = "test-resource-1";
    var w = TestUtils.loadWorkflow("/WorkflowClusterScopeResource.yaml");
    w = extension.create(w);

    await().untilAsserted(() -> {
      var clusterScopedResource =
          extension.get(ClusterScopeTestCustomResource.class, clusterScopedResourceName);
      assertThat(clusterScopedResource).isNotNull();
    });

    // Deletion not working with owner references since Workflow is not cluster scoped.
    // https://github.com/csviri/resource-workflow-operator/issues/4
    //
    // extension.delete(w);
    //
    // await().untilAsserted(() -> {
    // var clusterScopedResource =
    // extension.get(ClusterScopeTestCustomResource.class, clusterScopedResourceName);
    // assertThat(clusterScopedResource).isNull();
    // });

  }

  @Test
  void changingWorkflow() {
    ResourceFlow w = extension.create(TestUtils.loadWorkflow("/WorkflowToChange.yaml"));

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
      ResourceFlow w = TestUtils.loadWorkflow("/WorkflowTemplateForConcurrency.yaml");
      w.getMetadata().setName(w.getMetadata().getName() + index);
      res.add(w);
    });
    return res;
  }

}

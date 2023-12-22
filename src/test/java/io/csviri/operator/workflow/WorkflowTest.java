package io.csviri.operator.workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.csviri.operator.workflow.reconciler.WorkflowReconciler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class WorkflowTest {

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder().withReconciler(new WorkflowReconciler())
          .build();

  @SuppressWarnings("unchecked")
  @Test
  void javaScriptCondition() {
    Workflow workflow = TestUtils.loadWorkflow("/Workflow2ResourceAndCondition.yaml");
    workflow = extension.create(workflow);

    await().pollDelay(Duration.ofMillis(150)).untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap1");
      var cm2 = extension.get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNull();
    });

    Map<String, String> map = (Map<String, String>) workflow.getSpec().getResources()
        .get(0).getResource().getAdditionalProperties().get("data");
    map.put("createOther", "true");
    extension.replace(workflow);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap1");
      var cm2 = extension.get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();
    });

    extension.delete(workflow);
    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap1");
      var cm2 = extension.get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void templatingObject() {
    Workflow workflow = TestUtils.loadWorkflow("/WorkflowTemplating.yaml");
    workflow = extension.create(workflow);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "templconfigmap1");
      var cm2 = extension.get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();

      assertThat(cm2.getData().get("valueFromCM1")).isEqualTo("value1");
    });

    extension.delete(workflow);
    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "templconfigmap1");
      var cm2 = extension.get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void stringTemplate() {
    Workflow workflow = TestUtils.loadWorkflow("/WorkflowWithResourceTemplate.yaml");

    workflow = extension.create(workflow);

    await().timeout(Duration.ofSeconds(120)).untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "templconfigmap1");
      var cm2 = extension.get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();

      assertThat(cm2.getData().get("valueFromCM1")).isEqualTo("value1");
    });

    extension.delete(workflow);
    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "templconfigmap1");
      var cm2 = extension.get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void simpleConcurrencyTest() {
    int num = 2;
    List<Workflow> workflowList = testWorkflowList(num);

    workflowList.forEach(w -> {
      extension.create(w);
    });

    await().untilAsserted(() -> IntStream.range(0, num).forEach(index -> {
      var w = extension.get(Workflow.class, "testworkflow" + index);
      assertThat(w).isNotNull();
      var cm1 = extension.get(ConfigMap.class, "testworkflow" + index + "-1");
      var cm2 = extension.get(ConfigMap.class, "testworkflow" + index + "-2");

      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();
    }));

    workflowList.forEach(w -> {
      extension.delete(w);
    });
    await().untilAsserted(() -> IntStream.range(0, num).forEach(index -> {
      var w = extension.get(Workflow.class, "testworkflow" + index);
      assertThat(w).isNull();
    }));
  }

  private List<Workflow> testWorkflowList(int num) {
    List<Workflow> res = new ArrayList<>();
    IntStream.range(0, num).forEach(index -> {
      Workflow w = TestUtils.loadWorkflow("/WorkflowTemplateForConcurrency.yaml");
      w.getMetadata().setName(w.getMetadata().getName() + index);
      res.add(w);
    });
    return res;
  }

}

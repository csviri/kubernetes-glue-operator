package io.csviri.operator.workflow;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static io.csviri.operator.workflow.Utils.loadWorkflow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class WorkflowTest {

  @RegisterExtension
  static LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder().withReconciler(new WorkflowReconciler())
          .build();

  @SuppressWarnings("unchecked")
  @Test
  void testJavaScriptCondition() {
    Workflow workflow = loadWorkflow("/Workflow2ResourceAndCondition.yaml");
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

}

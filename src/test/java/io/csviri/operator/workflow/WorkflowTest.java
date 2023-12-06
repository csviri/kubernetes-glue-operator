package io.csviri.operator.workflow;

import java.time.Duration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static io.csviri.operator.workflow.Utils.loadWorkflow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class WorkflowTest {

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder().withReconciler(new WorkflowReconciler())
          .build();

  @Disabled
  @Test
  void testJavaScriptCondition() {
    Workflow workflow = loadWorkflow("/Workflow2ResourceAndCondition.yaml");
    extension.create(workflow);

    await().pollDelay(Duration.ofMillis(150)).untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configMap1");
      var cm2 = extension.get(ConfigMap.class, "configMap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNull();
    });
  }

}

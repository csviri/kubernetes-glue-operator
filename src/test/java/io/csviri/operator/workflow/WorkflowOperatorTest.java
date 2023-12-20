package io.csviri.operator.workflow;

import java.time.Duration;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.workflow.customresource.TestCustomResource;
import io.csviri.operator.workflow.customresource.TestCustomResourceSpec;
import io.csviri.operator.workflow.customresource.operator.WorkflowOperator;
import io.csviri.operator.workflow.customresource.operator.WorkflowOperatorSpec;
import io.csviri.operator.workflow.customresource.workflow.DependentResourceSpec;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static io.csviri.operator.workflow.customresource.TestCustomResource.CR_GROUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class WorkflowOperatorTest {

  public static final String TEST_RESOURCE_VALUE = "val1";

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder()
          .withReconciler(new WorkflowReconciler())
          .withReconciler(new WorkflowOperatorReconciler())
          .withAdditionalCustomResourceDefinition(TestCustomResource.class)
          .build();

  @Test
  void smokeTestWorkflowOperator() {
    extension.create(testWorkflowOperator());
    var cr = extension.create(testCustomResource());

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "test1");
      assertThat(cm1).isNotNull();
    });

    extension.delete(cr);

    await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "test1");
      assertThat(cm1).isNull();
    });
  }

  @Test
  void templating() {
    var wo = TestUtils.loadWorkflowOperator("/WorkflowOperatorTemplating.yaml");
    extension.create(wo);
    var cr = extension.create(testCustomResource());

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap-wo-templated");
      assertThat(cm1).isNotNull();
      assertThat(cm1.getData()).containsEntry("key", TEST_RESOURCE_VALUE);
    });

    extension.delete(cr);

    await().timeout(Duration.ofSeconds(5)).untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap-wo-templated");
      assertThat(cm1).isNull();
    });
  }

  TestCustomResource testCustomResource() {
    var res = new TestCustomResource();
    res.setMetadata(new ObjectMetaBuilder()
        .withName("testcr1")
        .build());
    res.setSpec(new TestCustomResourceSpec());
    res.getSpec().setValue(TEST_RESOURCE_VALUE);
    return res;
  }

  WorkflowOperator testWorkflowOperator() {
    var wo = new WorkflowOperator();
    wo.setMetadata(new ObjectMetaBuilder()
        .withName("wo1")
        .build());
    var spec = new WorkflowOperatorSpec();
    wo.setSpec(spec);
    spec.setGroup(CR_GROUP);
    spec.setVersion("v1");
    spec.setKind(TestCustomResource.class.getSimpleName());

    spec.setResources(new ArrayList<>());
    DependentResourceSpec drs = new DependentResourceSpec();
    spec.getResources().add(drs);
    drs.setResource(TestUtils.load("/ConfigMap.yaml"));
    drs.setName("configMap1");
    return wo;
  }

}

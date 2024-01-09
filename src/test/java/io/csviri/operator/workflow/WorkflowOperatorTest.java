package io.csviri.operator.workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.workflow.customresource.TestCustomResource;
import io.csviri.operator.workflow.customresource.TestCustomResource2;
import io.csviri.operator.workflow.customresource.TestCustomResourceSpec;
import io.csviri.operator.workflow.customresource.operator.Parent;
import io.csviri.operator.workflow.customresource.operator.WorkflowOperator;
import io.csviri.operator.workflow.customresource.operator.WorkflowOperatorSpec;
import io.csviri.operator.workflow.customresource.workflow.DependentResourceSpec;
import io.csviri.operator.workflow.reconciler.WorkflowOperatorReconciler;
import io.csviri.operator.workflow.reconciler.WorkflowReconciler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static io.csviri.operator.workflow.customresource.TestCustomResource.CR_GROUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class WorkflowOperatorTest {

  public static final String TEST_RESOURCE_VALUE = "val";
  public static final String TEST_RESOURCE_PREFIX = "testcr";
  public static final String TEST_RESOURCE2_PREFIX = "testcr2";

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder()
          .withReconciler(new WorkflowReconciler())
          .withReconciler(new WorkflowOperatorReconciler())
          .withAdditionalCustomResourceDefinition(TestCustomResource.class)
          .withAdditionalCustomResourceDefinition(TestCustomResource2.class)
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
      assertThat(cm1.getData()).containsEntry("key", cr.getSpec().getValue());
    });

    extension.delete(cr);

    await().untilAsserted(() -> {
      var cm1 = extension.get(ConfigMap.class, "configmap-wo-templated");
      assertThat(cm1).isNull();
    });
  }

  @Test
  void simpleConcurrencyTest() {
    int num = 10;
    extension.create(TestUtils.loadWorkflowOperator("/WorkflowOperatorConcurrency.yaml"));

    var resources =
        IntStream.range(0, num).mapToObj(n -> extension.create(testCustomResource(n))).toList();

    await().untilAsserted(() -> IntStream.range(0, num).forEach(n -> {
      var cm = extension.get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
      assertThat(cm).isNotNull();
      assertThat(cm.getData()).containsEntry("key", TEST_RESOURCE_VALUE + n);
    }));

    resources.forEach(r -> extension.delete(r));

    await().untilAsserted(() -> IntStream.range(0, num).forEach(n -> {
      var cm = extension.get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
      assertThat(cm).isNull();
    }));
  }

  @Test
  void simpleConcurrencyForMultipleOperatorTest() {
    int num = 10;
    extension.create(TestUtils.loadWorkflowOperator("/WorkflowOperatorConcurrency.yaml"));
    extension.create(TestUtils.loadWorkflowOperator("/WorkflowOperatorConcurrency2.yaml"));

    var crs =
        IntStream.range(0, num).mapToObj(n -> extension.create(testCustomResource(n))).toList();
    var cr2s =
        IntStream.range(0, num).mapToObj(n -> extension.create(testCustomResource2(n))).toList();

    await().untilAsserted(() -> IntStream.range(0, num).forEach(n -> {
      var cm = extension.get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
      assertThat(cm).isNotNull();
      assertThat(cm.getData()).containsEntry("key", TEST_RESOURCE_VALUE + n);

      var cm2 = extension.get(ConfigMap.class, TEST_RESOURCE2_PREFIX + n);
      assertThat(cm2).isNotNull();
      assertThat(cm2.getData()).containsEntry("key", TEST_RESOURCE_VALUE + n);
    }));

    crs.forEach(r -> extension.delete(r));
    cr2s.forEach(r -> extension.delete(r));

    await().untilAsserted(() -> {
      IntStream.range(0, num).forEach(n -> {
        var cm = extension.get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
        assertThat(cm).isNull();
        var cm2 = extension.get(ConfigMap.class, TEST_RESOURCE2_PREFIX + n);
        assertThat(cm2).isNull();
      });
    });
  }

  TestCustomResource testCustomResource() {
    return testCustomResource(1);
  }

  TestCustomResource testCustomResource(int index) {
    var res = new TestCustomResource();
    res.setMetadata(new ObjectMetaBuilder()
        .withName(TEST_RESOURCE_PREFIX + index)
        .build());
    res.setSpec(new TestCustomResourceSpec());
    res.getSpec().setValue(TEST_RESOURCE_VALUE + index);
    return res;
  }

  TestCustomResource2 testCustomResource2() {
    return testCustomResource2(1);
  }

  TestCustomResource2 testCustomResource2(int index) {
    var res = new TestCustomResource2();
    res.setMetadata(new ObjectMetaBuilder()
        .withName(TEST_RESOURCE2_PREFIX + index)
        .build());
    res.setSpec(new TestCustomResourceSpec());
    res.getSpec().setValue(TEST_RESOURCE_VALUE + index);
    return res;
  }

  WorkflowOperator testWorkflowOperator() {
    var wo = new WorkflowOperator();
    wo.setMetadata(new ObjectMetaBuilder()
        .withName("wo1")
        .build());
    var spec = new WorkflowOperatorSpec();
    wo.setSpec(spec);
    spec.setParent(new Parent(CR_GROUP + "/v1", TestCustomResource.class.getSimpleName()));

    spec.setResources(new ArrayList<>());
    DependentResourceSpec drs = new DependentResourceSpec();
    spec.getResources().add(drs);
    drs.setResource(TestUtils.load("/ConfigMap.yaml"));
    drs.setName("configMap1");
    return wo;
  }

}

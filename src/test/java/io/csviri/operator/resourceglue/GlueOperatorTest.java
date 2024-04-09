package io.csviri.operator.resourceglue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.csviri.operator.resourceglue.customresource.TestCustomResource;
import io.csviri.operator.resourceglue.customresource.TestCustomResource2;
import io.csviri.operator.resourceglue.customresource.TestCustomResourceSpec;
import io.csviri.operator.resourceglue.customresource.glue.DependentResourceSpec;
import io.csviri.operator.resourceglue.customresource.operator.GlueOperator;
import io.csviri.operator.resourceglue.customresource.operator.Parent;
import io.csviri.operator.resourceglue.customresource.operator.ResourceGlueOperatorSpec;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;

import static io.csviri.operator.resourceglue.TestUtils.GC_WAIT_TIMEOUT_SECOND;
import static io.csviri.operator.resourceglue.customresource.TestCustomResource.CR_GROUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
class GlueOperatorTest extends TestBase {

  public static final String TEST_RESOURCE_VALUE = "val";
  public static final String TEST_RESOURCE_PREFIX = "testcr";
  public static final String TEST_RESOURCE2_PREFIX = "testcr2";

  @BeforeEach
  void applyCRD() {
    TestUtils.applyTestCrd(client, TestCustomResource.class, TestCustomResource2.class);
  }

  @Test
  void smokeTestResourceGlueOperator() {
    create(testWorkflowOperator());
    var cr = create(testCustomResource());

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "test1");
      assertThat(cm1).isNotNull();
    });

    delete(cr);

    await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "test1");
      var actualCR = get(TestCustomResource.class, cr.getMetadata().getName());
      assertThat(cm1).isNull();
      assertThat(actualCR).isNull();
    });
  }

  @Test
  void templating() {
    var wo = TestUtils
        .loadResourceFlowOperator("/resourceglueoperator/Templating.yaml");
    create(wo);
    var cr = create(testCustomResource());
    String initialValue = cr.getSpec().getValue();
    String name = cr.getMetadata().getName();

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, name);
      assertThat(cm1).isNotNull();
      assertThat(cm1.getData()).containsEntry("key", initialValue);
    });

    var changedValue = "changed-value";
    cr.getSpec().setValue(changedValue);
    cr = update(cr);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, name);
      assertThat(cm1.getData()).containsEntry("key", changedValue);
    });

    delete(cr);

    await().timeout(Duration.ofMinutes(5)).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, name);
      var actualCR = get(TestCustomResource.class, name);
      assertThat(cm1).isNull();
      assertThat(actualCR).isNull();
    });
  }


  @Test
  void simpleConcurrencyTest() {
    int num = 10;
    create(TestUtils
        .loadResourceFlowOperator("/resourceglueoperator/Concurrency.yaml"));

    var resources =
        IntStream.range(0, num).mapToObj(n -> create(testCustomResource(n))).toList();

    await().untilAsserted(() -> IntStream.range(0, num).forEach(n -> {
      var cm = get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
      assertThat(cm).isNotNull();
      assertThat(cm.getData()).containsEntry("key", TEST_RESOURCE_VALUE + n);
    }));

    resources.forEach(this::delete);

    await().untilAsserted(() -> IntStream.range(0, num).forEach(n -> {
      var cm = get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
      assertThat(cm).isNull();
    }));
  }


  @Test
  void simpleConcurrencyForMultipleOperatorTest() {
    int num = 10;
    create(TestUtils
        .loadResourceFlowOperator("/resourceglueoperator/Concurrency.yaml"));
    create(TestUtils
        .loadResourceFlowOperator("/resourceglueoperator/Concurrency2.yaml"));

    var crs =
        IntStream.range(0, num).mapToObj(n -> create(testCustomResource(n))).toList();
    var cr2s =
        IntStream.range(0, num).mapToObj(n -> create(testCustomResource2(n))).toList();

    await().untilAsserted(() -> IntStream.range(0, num).forEach(n -> {
      var cm = get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
      assertThat(cm).isNotNull();
      assertThat(cm.getData()).containsEntry("key", TEST_RESOURCE_VALUE + n);

      var cm2 = get(ConfigMap.class, TEST_RESOURCE2_PREFIX + n);
      assertThat(cm2).isNotNull();
      assertThat(cm2.getData()).containsEntry("key", TEST_RESOURCE_VALUE + n);
    }));

    crs.forEach(this::delete);
    cr2s.forEach(this::delete);

    await().timeout(GC_WAIT_TIMEOUT_SECOND)
        .untilAsserted(() -> IntStream.range(0, num).forEach(n -> {
          var cm = get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
          assertThat(cm).isNull();
          var cm2 = get(ConfigMap.class, TEST_RESOURCE2_PREFIX + n);
          assertThat(cm2).isNull();
        }));
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

  TestCustomResource2 testCustomResource2(int index) {
    var res = new TestCustomResource2();
    res.setMetadata(new ObjectMetaBuilder()
        .withName(TEST_RESOURCE2_PREFIX + index)
        .build());
    res.setSpec(new TestCustomResourceSpec());
    res.getSpec().setValue(TEST_RESOURCE_VALUE + index);
    return res;
  }

  GlueOperator testWorkflowOperator() {
    var wo = new GlueOperator();
    wo.setMetadata(new ObjectMetaBuilder()
        .withName("wo1")
        .build());
    var spec = new ResourceGlueOperatorSpec();
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

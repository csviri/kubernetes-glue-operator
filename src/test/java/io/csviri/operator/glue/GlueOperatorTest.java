package io.csviri.operator.glue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.csviri.operator.glue.customresource.TestCustomResource;
import io.csviri.operator.glue.customresource.TestCustomResource2;
import io.csviri.operator.glue.customresource.glue.DependentResourceSpec;
import io.csviri.operator.glue.customresource.operator.GlueOperator;
import io.csviri.operator.glue.customresource.operator.GlueOperatorSpec;
import io.csviri.operator.glue.customresource.operator.Parent;
import io.csviri.operator.glue.reconciler.ValidationAndErrorHandler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;

import static io.csviri.operator.glue.TestData.*;
import static io.csviri.operator.glue.TestUtils.GC_WAIT_TIMEOUT;
import static io.csviri.operator.glue.customresource.TestCustomResource.CR_GROUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
class GlueOperatorTest extends TestBase {

  @BeforeEach
  void applyCRD() {
    TestUtils.applyTestCrd(client, TestCustomResource.class, TestCustomResource2.class);
  }

  @Test
  void smokeTest() {
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
    create(TestUtils
        .loadGlueOperator("/glueoperator/SimpleGlueOperator.yaml"));

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

    await().timeout(GC_WAIT_TIMEOUT).untilAsserted(() -> {
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
        .loadGlueOperator("/glueoperator/Concurrency.yaml"));

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
        .loadGlueOperator("/glueoperator/Concurrency.yaml"));
    create(TestUtils
        .loadGlueOperator("/glueoperator/Concurrency2.yaml"));

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

    await().timeout(GC_WAIT_TIMEOUT)
        .untilAsserted(() -> IntStream.range(0, num).forEach(n -> {
          var cm = get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
          assertThat(cm).isNull();
          var cm2 = get(ConfigMap.class, TEST_RESOURCE2_PREFIX + n);
          assertThat(cm2).isNull();
        }));
  }

  @Test
  void nonUniqueNameTest() {
    var go = create(TestUtils
        .loadGlueOperator("/glueoperator/NonUniqueName.yaml"));

    await().untilAsserted(() -> {
      var actual = get(GlueOperator.class, go.getMetadata().getName());

      assertThat(actual.getStatus()).isNotNull();
      assertThat(actual.getStatus().getErrorMessage())
          .startsWith(ValidationAndErrorHandler.NON_UNIQUE_NAMES_FOUND_PREFIX);
    });
  }

  @Test
  void parentWithLabelSelector() {
    create(TestUtils
        .loadGlueOperator("/glueoperator/ParentLabelSelector.yaml"));

    var cr = create(testCustomResource());
    String name = cr.getMetadata().getName();

    await().pollDelay(TestUtils.INITIAL_RECONCILE_WAIT_TIMEOUT).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, name);
      assertThat(cm1).isNull();
    });

    cr.getMetadata().getLabels().put("mylabel", "value");
    update(cr);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, name);
      assertThat(cm1).isNotNull();
    });

    delete(cr);
    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, name);
      assertThat(cm1).isNull();
    });
  }


  @Test
  void secretCopySample() {

  }


  GlueOperator testWorkflowOperator() {
    var wo = new GlueOperator();
    wo.setMetadata(new ObjectMetaBuilder()
        .withName("wo1")
        .build());
    var spec = new GlueOperatorSpec();
    wo.setSpec(spec);
    spec.setParent(new Parent(CR_GROUP + "/v1", TestCustomResource.class.getSimpleName()));

    spec.setChildResources(new ArrayList<>());
    DependentResourceSpec drs = new DependentResourceSpec();
    spec.getChildResources().add(drs);
    drs.setResource(TestUtils.load("/ConfigMap.yaml"));
    drs.setName("configMap1");
    return wo;
  }

}

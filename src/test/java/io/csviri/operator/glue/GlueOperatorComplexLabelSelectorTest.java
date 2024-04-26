package io.csviri.operator.glue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.csviri.operator.glue.customresource.TestCustomResource;
import io.csviri.operator.glue.customresource.TestCustomResource2;
import io.csviri.operator.glue.customresource.glue.Glue;
import io.csviri.operator.glue.reconciler.operator.GlueOperatorReconciler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static io.csviri.operator.glue.reconciler.glue.GlueReconciler.GLUE_RECONCILER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@TestProfile(GlueOperatorComplexLabelSelectorTest.GlueOperatorComplexLabelSelectorTestProfile.class)
public class GlueOperatorComplexLabelSelectorTest extends TestBase {

  public static final String GLUE_LABEL_KEY1 = "test-glue1";
  public static final String GLUE_LABEL_KEY2 = "test-glue2";
  public static final String LABEL_VALUE = "true";

  @BeforeEach
  void applyCRD() {
    TestUtils.applyTestCrd(client, TestCustomResource.class, TestCustomResource2.class);
  }

  @Test
  void testGlueOperatorLabelSelector() {
    var go = create(TestUtils
        .loadResourceFlowOperator("/glueoperator/SimpleGlueOperator.yaml"));

    var testCR = create(TestData.testCustomResource());
    
    await().untilAsserted(() -> {
      assertThat(get(ConfigMap.class, testCR.getMetadata().getName())).isNotNull();
      var glue = get(Glue.class, GlueOperatorReconciler.glueName(testCR.getMetadata().getName(),
          testCR.getKind()));
      assertThat(glue).isNotNull();
      assertThat(glue.getMetadata().getLabels())
          .containsEntry(GLUE_LABEL_KEY1, LABEL_VALUE)
          .containsEntry(GLUE_LABEL_KEY2, LABEL_VALUE);
    });

    delete(testCR);
    await().untilAsserted(() -> {
      var glue = get(Glue.class, GlueOperatorReconciler.glueName(testCR.getMetadata().getName(),
          testCR.getKind()));
      assertThat(glue).isNull();
    });
    delete(go);
  }

  public static class GlueOperatorComplexLabelSelectorTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("quarkus.operator-sdk.controllers." + GLUE_RECONCILER_NAME + ".selector",
          // complex label selector with 2 values checked
          GLUE_LABEL_KEY1 + "=" + LABEL_VALUE + "," + GLUE_LABEL_KEY2 + "=" + LABEL_VALUE,
          // explicit labels added to glue
          "glue.operator.glue-operator-managed-glue-labels." + GLUE_LABEL_KEY1, LABEL_VALUE,
          "glue.operator.glue-operator-managed-glue-labels." + GLUE_LABEL_KEY2, LABEL_VALUE);
    }
  }

}

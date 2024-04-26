package io.csviri.operator.glue;


import java.util.Map;

import org.junit.jupiter.api.Test;

import io.csviri.operator.glue.customresource.glue.Glue;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static io.csviri.operator.glue.TestUtils.INITIAL_RECONCILE_WAIT_TIMEOUT;
import static io.csviri.operator.glue.reconciler.glue.GlueReconciler.GLUE_RECONCILER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@TestProfile(GlueLabelSelectorTest.LabelSelectorTestProfile.class)
public class GlueLabelSelectorTest extends TestBase {


  public static final String LABEL_KEY = "test-glue";
  public static final String LABEL_VALUE = "true";

  @Test
  void testLabelSelectorHandling() {
    Glue glue =
        TestUtils.loadResoureFlow("/glue/SimpleGlue.yaml");
    glue = create(glue);

    await().pollDelay(INITIAL_RECONCILE_WAIT_TIMEOUT).untilAsserted(() -> {
      assertThat(get(ConfigMap.class, "simple-glue-configmap")).isNull();
    });

    glue.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
    update(glue);

    await().untilAsserted(() -> {
      assertThat(get(ConfigMap.class, "simple-glue-configmap")).isNotNull();
    });
  }

  public static class LabelSelectorTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("quarkus.operator-sdk.controllers." + GLUE_RECONCILER_NAME + ".selector",
          LABEL_KEY + "=" + LABEL_VALUE);
    }
  }

}

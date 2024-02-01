package io.csviri.operator.resourceflow.sample.webpage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.resourceflow.TestUtils;
import io.csviri.operator.resourceflow.reconciler.flow.ResourceFlowReconciler;
import io.csviri.operator.resourceflow.reconciler.operator.ResourceFlowOperatorReconciler;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class WebPageSampleTest {

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder()
          .withReconciler(new ResourceFlowReconciler())
          .withReconciler(new ResourceFlowOperatorReconciler())
          .build();

  @Test
  void webPageCRUD() {
    TestUtils.createOrUpdate(extension, "/sample/webpage/webpage.crd.yml");
    TestUtils.createOrUpdate(extension, "/sample/webpage/webpage.operator.yaml");
    var webPage = TestUtils.createOrUpdate(extension, "/sample/webpage/webpage.yaml");

    await().untilAsserted(() -> {
      var deployment = extension.get(Deployment.class, webPage.getMetadata().getName());

      assertThat(deployment).isNotNull();
    });
    // todo JS condition error
    extension.delete(webPage);

    await().untilAsserted(() -> {
      var deployment = extension.get(Deployment.class, webPage.getMetadata().getName());

      assertThat(deployment).isNull();
    });
  }


}

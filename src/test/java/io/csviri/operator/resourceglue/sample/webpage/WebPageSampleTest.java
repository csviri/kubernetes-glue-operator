package io.csviri.operator.resourceglue.sample.webpage;

import java.util.HashMap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.resourceglue.TestUtils;
import io.csviri.operator.resourceglue.reconciler.glue.ResourceGlueReconciler;
import io.csviri.operator.resourceglue.reconciler.operator.GlueOperatorReconciler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Disabled
public class WebPageSampleTest {

  @RegisterExtension
  LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder()
          .withReconciler(new ResourceGlueReconciler())
          .withReconciler(new GlueOperatorReconciler())
          .build();

  @Test
  void webPageCRUD() {
    TestUtils.createOrUpdate(extension, "/sample/webpage/webpage.crd.yml");
    TestUtils.createOrUpdate(extension, "/sample/webpage/webpage.operator.yaml");
    var webPage = TestUtils.createOrUpdate(extension, "/sample/webpage/webpage.yaml");

    await().untilAsserted(() -> {
      var deployment = extension.get(Deployment.class, webPage.getMetadata().getName());
      var configMap = extension.get(ConfigMap.class, webPage.getMetadata().getName());
      var service = extension.get(Service.class, webPage.getMetadata().getName());
      var ingress = extension.get(Ingress.class, webPage.getMetadata().getName());

      assertThat(ingress).isNull();
      assertThat(deployment).isNotNull();
      assertThat(service).isNotNull();
      assertThat(configMap).isNotNull();
      assertThat(configMap.getData().get("index.html")).contains("Hello World!");
    });

    setExposed(webPage);
    setNewHtml(webPage);
    extension.replace(webPage);

    await().untilAsserted(() -> {
      var ingress = extension.get(Ingress.class, webPage.getMetadata().getName());
      var configMap = extension.get(ConfigMap.class, webPage.getMetadata().getName());

      assertThat(configMap.getData().get("index.html")).contains("Hello World 2!");
      assertThat(ingress).isNotNull();
    });

    extension.delete(webPage);

    await().untilAsserted(() -> {
      var deployment = extension.get(Deployment.class, webPage.getMetadata().getName());
      assertThat(deployment).isNull();
    });
  }

  private void setNewHtml(GenericKubernetesResource webPage) {
    ((HashMap<String, Object>) webPage.getAdditionalProperties().get("spec")).put("html", """
        <html>
              <head>
                <title>Hello Operator World</title>
              </head>
              <body>
                Hello World 2!
              </body>
            </html>
        """);
  }

  private void setExposed(GenericKubernetesResource webPage) {
    ((HashMap<String, Object>) webPage.getAdditionalProperties().get("spec")).put("exposed", true);
  }
}

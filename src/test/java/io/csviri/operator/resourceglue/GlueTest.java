package io.csviri.operator.resourceglue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import io.csviri.operator.resourceglue.customresource.glue.DependentResourceSpec;
import org.junit.jupiter.api.Test;

import io.csviri.operator.resourceglue.customresource.glue.Glue;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
class GlueTest {

    public static final String CHANGED_VALUE = "changed_value";

    @Inject
    KubernetesClient client;

    @Test
    void simpleTemplating() {
        Glue glue =
                TestUtils.loadResoureFlow("/resourceglue/Templating.yaml");
        glue = client.resource(glue).create();

        await().untilAsserted(() -> {
            var cm1 = client.configMaps().withName("templconfigmap1").get();
            var cm2 = client.configMaps().withName("templconfigmap2").get();
            assertThat(cm1).isNotNull();
            assertThat(cm2).isNotNull();

            assertThat(cm2.getData().get("valueFromCM1")).isEqualTo("value1");
        });

        ((Map<String, String>) glue.getSpec().getResources().get(0).getResource()
                .getAdditionalProperties().get("data"))
                .put("key", CHANGED_VALUE);


        glue.getMetadata().setResourceVersion(null);
        client.resource(glue).update();

        await().untilAsserted(() -> {
            var cm2 = client.configMaps().withName("templconfigmap2").get();
            assertThat(cm2.getData().get("valueFromCM1")).isEqualTo(CHANGED_VALUE);
        });

        client.resource(glue).delete();
        await().timeout(Duration.ofSeconds(30)).untilAsserted(() -> {
            var cm1 = client.configMaps().withName("templconfigmap1").get();
            var cm2 = client.configMaps().withName("templconfigmap2").get();
            assertThat(cm1).isNull();
            assertThat(cm2).isNull();
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    void javaScriptCondition() {
        Glue glue =
                TestUtils.loadResoureFlow("/resourceglue/TwoResourcesAndCondition.yaml");
        glue = client.resource(glue).create();

        await().pollDelay(Duration.ofMillis(150)).untilAsserted(() -> {
            var cm1 = client.configMaps().withName("configmap1").get();
            var cm2 = client.configMaps().withName("configmap2").get();
            assertThat(cm1).isNotNull();
            assertThat(cm2).isNull();
        });

        Map<String, String> map = (Map<String, String>) glue.getSpec().getResources()
                .get(0).getResource().getAdditionalProperties().get("data");
        map.put("createOther", "true");

        glue.getMetadata().setResourceVersion(null);
        client.resource(glue).update();

        await().untilAsserted(() -> {
            var cm1 = client.configMaps().withName("configmap1").get();
            var cm2 = client.configMaps().withName("configmap2").get();
            assertThat(cm1).isNotNull();
            assertThat(cm2).isNotNull();
        });

        client.resource(glue).delete();
        await().untilAsserted(() -> {
            var cm1 = client.configMaps().withName("configmap1").get();
            var cm2 = client.configMaps().withName("configmap2").get();
            assertThat(cm1).isNull();
            assertThat(cm2).isNull();
        });
    }

   @Test
   void stringTemplate() {
   Glue glue =
   TestUtils.loadResoureFlow("/resourceglue/ResourceTemplate.yaml");

   glue = client.resource(glue).create();

   await().timeout(Duration.ofSeconds(120)).untilAsserted(() -> {
       var cm1 = client.configMaps().withName("templconfigmap1").get();
       var cm2 = client.configMaps().withName("templconfigmap2").get();
        assertThat(cm1).isNotNull();
        assertThat(cm2).isNotNull();

        assertThat(cm2.getData().get("valueFromCM1")).isEqualTo("value1");
   });

   client.resource(glue).delete();
   await().untilAsserted(() -> {
      var cm1 = client.configMaps().withName("templconfigmap1").get();
      var cm2 = client.configMaps().withName("templconfigmap2").get();
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
   });
   }

     @Test
     void simpleConcurrencyTest() {
     int num = 10;
     List<Glue> glueList = testWorkflowList(num);

     glueList.forEach(w -> {
     client.resource(w).create();
     });

     await().untilAsserted(() -> IntStream.range(0, num).forEach(index -> {
     var w = client.resources(Glue.class).withName("testglue" + index).get();
     assertThat(w).isNotNull();
     var cm1 = client.configMaps().withName("testglue" + index + "-1").get();
     var cm2 = client.configMaps().withName("testglue" + index + "-2").get();

     assertThat(cm1).isNotNull();
     assertThat(cm2).isNotNull();
     }));

     glueList.forEach(w -> {
        client.resource(w).delete(w);
     });
     await().untilAsserted(() -> IntStream.range(0, num).forEach(index -> {
      var w=  client.resources(Glue.class).withName("testglue" + index).get();

     assertThat(w).isNull();
     }));
     }

     @Test
     void changingWorkflow() {
     Glue w =
     client.resource(TestUtils.loadResoureFlow("/resourceglue/ChanginResources.yaml")).create();

     await().untilAsserted(() -> {
     var cm1 = client.configMaps().withName("configmap1").get();
     var cm2 = client.configMaps().withName("configmap2").get();
     assertThat(cm1).isNotNull();
     assertThat(cm2).isNotNull();
     });

     w.getSpec().getResources().remove(1);
     w.getSpec().getResources().add(new DependentResourceSpec()
     .setName("secret")
     .setResource(TestUtils.load("/Secret.yaml")));
     w = client.resource(w).update();

     await().untilAsserted(() -> {
         var cm1 = client.configMaps().withName("configmap1").get();
         var cm2 = client.configMaps().withName("configmap2").get();
     var s = client.secrets().withName("secret1").get();
     assertThat(cm1).isNotNull();
     assertThat(cm2).isNull();
     assertThat(s).isNotNull();
     });

     w.getMetadata().setResourceVersion(null);
     client.resource(w).delete();

     await().untilAsserted(() -> {
         var cm1 = client.configMaps().withName("configmap1").get();
         var s = client.secrets().withName("secret1").get();
     assertThat(cm1).isNull();
     assertThat(s).isNull();
     });
     }
    //
//     @Disabled("Not supported in current version")
//     @Test
//     void childInDifferentNamespaceAsPrimary() {
//     Glue w = extension
//     .create(TestUtils.loadResoureFlow("/resourceglue/ResourceInDifferentNamespace.yaml"));
//
//     await().untilAsserted(() -> {
//     var cmDifferentNS = extension.getKubernetesClient().configMaps().inNamespace("default")
//     .withName("configmap1");
//     var cm2 = extension.get(ConfigMap.class, "configmap2");
//
//     assertThat(cmDifferentNS).isNotNull();
//     assertThat(cm2).isNotNull();
//     });
//
//     extension.delete(w);
//
//     await().untilAsserted(() -> {
//     var cmDifferentNS = extension.getKubernetesClient().configMaps().inNamespace("default")
//     .withName("configmap1");
//     var cm2 = extension.get(ConfigMap.class, "configmap2");
//
//     assertThat(cmDifferentNS).isNull();
//     assertThat(cm2).isNull();
//     });
//
//     }

     private List<Glue> testWorkflowList(int num) {
          List<Glue> res = new ArrayList<>();
             IntStream.range(0, num).forEach(index -> {
     Glue w =
     TestUtils.loadResoureFlow("/resourceglue/TemplateForConcurrency.yaml");
     w.getMetadata().setName(w.getMetadata().getName() + index);
     res.add(w);
     });
     return res;
     }

}

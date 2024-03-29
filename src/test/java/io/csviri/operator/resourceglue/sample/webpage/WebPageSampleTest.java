package io.csviri.operator.resourceglue.sample.webpage;

public class WebPageSampleTest {

  // todo   

  // @RegisterExtension
  // LocallyRunOperatorExtension extension =
  // LocallyRunOperatorExtension.builder()
  // .withReconciler(new GlueReconciler())
  // .withReconciler(new GlueOperatorReconciler())
  // .build();
  //
  // @Test
  // void webPageCRUD() {
  // TestUtils.createOrUpdate(extension, "/sample/webpage/webpage.crd.yml");
  // TestUtils.createOrUpdate(extension, "/sample/webpage/webpage.operator.yaml");
  // var webPage = TestUtils.createOrUpdate(extension, "/sample/webpage/webpage.sample.yaml");
  //
  // await().untilAsserted(() -> {
  // var deployment = extension.get(Deployment.class, webPage.getMetadata().getName());
  // var configMap = extension.get(ConfigMap.class, webPage.getMetadata().getName());
  // var service = extension.get(Service.class, webPage.getMetadata().getName());
  // var ingress = extension.get(Ingress.class, webPage.getMetadata().getName());
  //
  // assertThat(ingress).isNull();
  // assertThat(deployment).isNotNull();
  // assertThat(service).isNotNull();
  // assertThat(configMap).isNotNull();
  // assertThat(configMap.getData().get("index.html")).contains("Hello World!");
  // });
  //
  // setExposed(webPage);
  // setNewHtml(webPage);
  // extension.replace(webPage);
  //
  // await().untilAsserted(() -> {
  // var ingress = extension.get(Ingress.class, webPage.getMetadata().getName());
  // var configMap = extension.get(ConfigMap.class, webPage.getMetadata().getName());
  //
  // assertThat(configMap.getData().get("index.html")).contains("Hello World 2!");
  // assertThat(ingress).isNotNull();
  // });
  //
  // extension.delete(webPage);
  //
  // await().untilAsserted(() -> {
  // var deployment = extension.get(Deployment.class, webPage.getMetadata().getName());
  // assertThat(deployment).isNull();
  // });
  // }
  //
  // private void setNewHtml(GenericKubernetesResource webPage) {
  // ((HashMap<String, Object>) webPage.getAdditionalProperties().get("spec")).put("html", """
  // <html>
  // <head>
  // <title>Hello Operator World</title>
  // </head>
  // <body>
  // Hello World 2!
  // </body>
  // </html>
  // """);
  // }
  //
  // private void setExposed(GenericKubernetesResource webPage) {
  // ((HashMap<String, Object>) webPage.getAdditionalProperties().get("spec")).put("exposed", true);
  // }
}

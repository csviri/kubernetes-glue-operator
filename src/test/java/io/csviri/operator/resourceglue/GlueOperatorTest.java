package io.csviri.operator.resourceglue;

class GlueOperatorTest {

  public static final String TEST_RESOURCE_VALUE = "val";
  public static final String TEST_RESOURCE_PREFIX = "testcr";
  public static final String TEST_RESOURCE2_PREFIX = "testcr2";


  // @RegisterExtension
  // LocallyRunOperatorExtension extension =
  // LocallyRunOperatorExtension.builder()
  // .withReconciler(new GlueReconciler())
  // .withReconciler(new GlueOperatorReconciler())
  // .withAdditionalCustomResourceDefinition(TestCustomResource.class)
  // .withAdditionalCustomResourceDefinition(TestCustomResource2.class)
  // .build();
  //
  // @Test
  // void smokeTestResourceGlueOperator() {
  // extension.create(testWorkflowOperator());
  // var cr = extension.create(testCustomResource());
  //
  // await().untilAsserted(() -> {
  // var cm1 = extension.get(ConfigMap.class, "test1");
  // assertThat(cm1).isNotNull();
  // });
  //
  // extension.delete(cr);
  //
  // await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
  // var cm1 = extension.get(ConfigMap.class, "test1");
  // var actualCR = extension.get(TestCustomResource.class, cr.getMetadata().getName());
  // assertThat(cm1).isNull();
  // assertThat(actualCR).isNull();
  // });
  // }
  //
  // @Test
  // void templating() {
  // var wo = TestUtils
  // .loadResourceFlowOperator("/resourceglueoperator/Templating.yaml");
  // extension.create(wo);
  // var cr = extension.create(testCustomResource());
  // String initialValue = cr.getSpec().getValue();
  // String name = cr.getMetadata().getName();
  //
  // await().untilAsserted(() -> {
  // var cm1 = extension.get(ConfigMap.class, name);
  // assertThat(cm1).isNotNull();
  // assertThat(cm1.getData()).containsEntry("key", initialValue);
  // });
  //
  // var changedValue = "changed-value";
  // cr.getSpec().setValue(changedValue);
  // cr = extension.replace(cr);
  //
  // await().untilAsserted(() -> {
  // var cm1 = extension.get(ConfigMap.class, name);
  // assertThat(cm1.getData()).containsEntry("key", changedValue);
  // });
  //
  // extension.delete(cr);
  //
  // await().untilAsserted(() -> {
  // var cm1 = extension.get(ConfigMap.class, name);
  // var actualCR = extension.get(TestCustomResource.class, name);
  // assertThat(cm1).isNull();
  // assertThat(actualCR).isNull();
  // });
  // }
  //
  // @Test
  // void simpleConcurrencyTest() {
  // int num = 10;
  // extension.create(TestUtils
  // .loadResourceFlowOperator("/resourceglueoperator/Concurrency.yaml"));
  //
  // var resources =
  // IntStream.range(0, num).mapToObj(n -> extension.create(testCustomResource(n))).toList();
  //
  // await()
  // .untilAsserted(() -> IntStream.range(0, num).forEach(n -> {
  // var cm = extension.get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
  // assertThat(cm).isNotNull();
  // assertThat(cm.getData()).containsEntry("key", TEST_RESOURCE_VALUE + n);
  // }));
  //
  // resources.forEach(r -> extension.delete(r));
  //
  // await()
  // .untilAsserted(() -> IntStream.range(0, num).forEach(n -> {
  // var cm = extension.get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
  // assertThat(cm).isNull();
  // }));
  // }
  //
  // @Test
  // void simpleConcurrencyForMultipleOperatorTest() {
  // int num = 10;
  // extension.create(TestUtils
  // .loadResourceFlowOperator("/resourceglueoperator/Concurrency.yaml"));
  // extension.create(TestUtils
  // .loadResourceFlowOperator("/resourceglueoperator/Concurrency2.yaml"));
  //
  // var crs =
  // IntStream.range(0, num).mapToObj(n -> extension.create(testCustomResource(n))).toList();
  // var cr2s =
  // IntStream.range(0, num).mapToObj(n -> extension.create(testCustomResource2(n))).toList();
  //
  // await().untilAsserted(() -> IntStream.range(0, num).forEach(n -> {
  // var cm = extension.get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
  // assertThat(cm).isNotNull();
  // assertThat(cm.getData()).containsEntry("key", TEST_RESOURCE_VALUE + n);
  //
  // var cm2 = extension.get(ConfigMap.class, TEST_RESOURCE2_PREFIX + n);
  // assertThat(cm2).isNotNull();
  // assertThat(cm2.getData()).containsEntry("key", TEST_RESOURCE_VALUE + n);
  // }));
  //
  // crs.forEach(r -> extension.delete(r));
  // cr2s.forEach(r -> extension.delete(r));
  //
  // await().untilAsserted(() -> {
  // IntStream.range(0, num).forEach(n -> {
  // var cm = extension.get(ConfigMap.class, TEST_RESOURCE_PREFIX + n);
  // assertThat(cm).isNull();
  // var cm2 = extension.get(ConfigMap.class, TEST_RESOURCE2_PREFIX + n);
  // assertThat(cm2).isNull();
  // });
  // });
  // }
  //
  // TestCustomResource testCustomResource() {
  // return testCustomResource(1);
  // }
  //
  // TestCustomResource testCustomResource(int index) {
  // var res = new TestCustomResource();
  // res.setMetadata(new ObjectMetaBuilder()
  // .withName(TEST_RESOURCE_PREFIX + index)
  // .build());
  // res.setSpec(new TestCustomResourceSpec());
  // res.getSpec().setValue(TEST_RESOURCE_VALUE + index);
  // return res;
  // }
  //
  // TestCustomResource2 testCustomResource2(int index) {
  // var res = new TestCustomResource2();
  // res.setMetadata(new ObjectMetaBuilder()
  // .withName(TEST_RESOURCE2_PREFIX + index)
  // .build());
  // res.setSpec(new TestCustomResourceSpec());
  // res.getSpec().setValue(TEST_RESOURCE_VALUE + index);
  // return res;
  // }
  //
  // GlueOperator testWorkflowOperator() {
  // var wo = new GlueOperator();
  // wo.setMetadata(new ObjectMetaBuilder()
  // .withName("wo1")
  // .build());
  // var spec = new ResourceGlueOperatorSpec();
  // wo.setSpec(spec);
  // spec.setParent(new Parent(CR_GROUP + "/v1", TestCustomResource.class.getSimpleName()));
  //
  // spec.setResources(new ArrayList<>());
  // DependentResourceSpec drs = new DependentResourceSpec();
  // spec.getResources().add(drs);
  // drs.setResource(TestUtils.load("/ConfigMap.yaml"));
  // drs.setName("configMap1");
  // return wo;
  // }

}

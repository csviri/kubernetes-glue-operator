package io.csviri.operator.glue.reconciler.glue;

import org.junit.jupiter.api.Test;

import io.javaoperatorsdk.operator.processing.GroupVersionKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class InformerRegisterTest {


  @Test
  void gvkToSimpleString() {
    assertThat(InformerRegister.toSimpleString(new GroupVersionKind("apps", "v1", "Deployment")))
        .isEqualTo("apps/v1#Deployment");
    assertThat(InformerRegister.toSimpleString(new GroupVersionKind("v1", "ConfigMap")))
        .isEqualTo("v1#ConfigMap");
  }

}

package io.csviri.operator.resourceglue;

import java.io.IOException;
import java.io.InputStream;

import io.csviri.operator.resourceglue.customresource.glue.Glue;
import io.csviri.operator.resourceglue.customresource.operator.GlueOperator;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.junit.AbstractOperatorExtension;

public class TestUtils {

  public static Glue loadResoureFlow(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, Glue.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static GlueOperator loadResourceFlowOperator(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, GlueOperator.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static GenericKubernetesResource load(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, GenericKubernetesResource.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static GenericKubernetesResource createOrUpdate(AbstractOperatorExtension extension,
      String path) {
    return extension.getKubernetesClient().resource(load(path))
        .inNamespace(extension.getNamespace())
        .createOr(NonDeletingOperation::update);
  }

  public static GenericKubernetesResource createOrUpdate(KubernetesClient client, String path) {
    return client.resource(load(path)).createOr(NonDeletingOperation::update);
  }
}

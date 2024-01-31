package io.csviri.operator.resourceflow;

import java.io.IOException;
import java.io.InputStream;

import io.csviri.operator.resourceflow.customresource.operator.ResourceFlowOperator;
import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.junit.AbstractOperatorExtension;

public class TestUtils {

  public static ResourceFlow loadResoureFlow(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, ResourceFlow.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static ResourceFlowOperator loadResourceFlowOperator(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, ResourceFlowOperator.class);
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
    return createOrUpdate(extension.getKubernetesClient(), path);
  }

  public static GenericKubernetesResource createOrUpdate(KubernetesClient client, String path) {
    return client.resource(load(path)).createOr(NonDeletingOperation::update);
  }
}

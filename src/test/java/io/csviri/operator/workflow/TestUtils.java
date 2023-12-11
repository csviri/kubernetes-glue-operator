package io.csviri.operator.workflow;

import java.io.IOException;
import java.io.InputStream;

import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;

public class TestUtils {

  public static Workflow loadWorkflow(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, Workflow.class);
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
}

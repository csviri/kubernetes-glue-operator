package io.csviri.operator.resourceflow;

import java.io.IOException;
import java.io.InputStream;

import io.csviri.operator.resourceflow.customresource.operator.ResourceFlowOperator;
import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;

public class TestUtils {

  public static ResourceFlow loadWorkflow(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, ResourceFlow.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static ResourceFlowOperator loadWorkflowOperator(String path) {
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
}

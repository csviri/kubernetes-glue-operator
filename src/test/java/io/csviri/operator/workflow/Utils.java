package io.csviri.operator.workflow;

import java.io.IOException;
import java.io.InputStream;

import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.client.utils.Serialization;

public class Utils {

  public static Workflow loadWorkflow(String path) {
    try (InputStream is = WorkflowSmokeTest.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, Workflow.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

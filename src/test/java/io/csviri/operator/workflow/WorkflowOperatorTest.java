package io.csviri.operator.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.csviri.operator.workflow.customresource.TestCustomResource;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

public class WorkflowOperatorTest {

  @RegisterExtension
  static LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder()
          .withReconciler(new WorkflowReconciler())
          .withReconciler(new WorkflowOperatorReconciler())
          .withAdditionalCustomResourceDefinition(TestCustomResource.class)
          .build();

  @Test
  void smokeTestWorkflowOperator() {

  }

}

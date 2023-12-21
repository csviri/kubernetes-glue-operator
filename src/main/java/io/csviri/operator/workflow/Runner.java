package io.csviri.operator.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.workflow.reconciler.WorkflowOperatorReconciler;
import io.csviri.operator.workflow.reconciler.WorkflowReconciler;
import io.javaoperatorsdk.operator.Operator;

public class Runner {

  private static final Logger log = LoggerFactory.getLogger(Runner.class);

  public static void main(String[] args) {
    Operator operator = new Operator();
    operator.register(new WorkflowReconciler());
    operator.register(new WorkflowOperatorReconciler());
    operator.start();
    log.info("Workflow controller started.");

  }
}

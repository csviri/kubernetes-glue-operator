package io.csviri.operator.resourceflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.resourceflow.reconciler.flow.ResourceFlowReconciler;
import io.csviri.operator.resourceflow.reconciler.operator.ResourceFlowOperatorReconciler;
import io.javaoperatorsdk.operator.Operator;

public class Runner {

  private static final Logger log = LoggerFactory.getLogger(Runner.class);

  public static void main(String[] args) {
    Operator operator = new Operator();
    operator.register(new ResourceFlowReconciler());
    operator.register(new ResourceFlowOperatorReconciler());
    operator.start();
    log.info("Workflow controller started.");

  }
}

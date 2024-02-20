package io.csviri.operator.resourceglue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.resourceglue.reconciler.glue.GlueReconciler;
import io.csviri.operator.resourceglue.reconciler.operator.GlueOperatorReconciler;
import io.javaoperatorsdk.operator.Operator;

public class Runner {

  private static final Logger log = LoggerFactory.getLogger(Runner.class);

  public static void main(String[] args) {
    Operator operator = new Operator();
    operator.register(new GlueReconciler());
    operator.register(new GlueOperatorReconciler());
    operator.start();
    log.info("resource-glue controller started.");
  }
}

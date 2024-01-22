package io.csviri.operator.resourceflow;

public class WorkflowException extends RuntimeException {

  public WorkflowException() {}

  public WorkflowException(String message) {
    super(message);
  }

  public WorkflowException(String message, Throwable cause) {
    super(message, cause);
  }

  public WorkflowException(Throwable cause) {
    super(cause);
  }

  public WorkflowException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

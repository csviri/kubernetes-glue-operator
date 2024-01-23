package io.csviri.operator.resourceflow;

public class ResourceFlowException extends RuntimeException {

  public ResourceFlowException() {}

  public ResourceFlowException(String message) {
    super(message);
  }

  public ResourceFlowException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResourceFlowException(Throwable cause) {
    super(cause);
  }

  public ResourceFlowException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

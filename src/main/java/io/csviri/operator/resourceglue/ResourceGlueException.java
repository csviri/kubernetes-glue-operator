package io.csviri.operator.resourceglue;

public class ResourceGlueException extends RuntimeException {

  public ResourceGlueException() {}

  public ResourceGlueException(String message) {
    super(message);
  }

  public ResourceGlueException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResourceGlueException(Throwable cause) {
    super(cause);
  }

  public ResourceGlueException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

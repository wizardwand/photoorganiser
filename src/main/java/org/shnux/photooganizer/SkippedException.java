package org.shnux.photooganizer;

public class SkippedException extends Exception {
  public SkippedException(String message) {
    super(message);
  }

  public SkippedException(String message, Throwable cause) {
    super(message, cause);
  }
}

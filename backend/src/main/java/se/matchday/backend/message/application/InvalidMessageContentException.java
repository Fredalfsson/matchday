package se.matchday.backend.message.application;

public final class InvalidMessageContentException extends IllegalArgumentException {

  public InvalidMessageContentException(IllegalArgumentException cause) {
    super(cause.getMessage(), cause);
  }
}

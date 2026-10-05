package se.matchday.backend.message.application;

public final class InvalidMessagePaginationException extends IllegalArgumentException {

  public InvalidMessagePaginationException(String message) {
    super(message);
  }
}

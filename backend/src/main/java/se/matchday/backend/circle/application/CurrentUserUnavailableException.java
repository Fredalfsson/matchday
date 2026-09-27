package se.matchday.backend.circle.application;

public final class CurrentUserUnavailableException extends RuntimeException {

  CurrentUserUnavailableException() {
    super("An authenticated user identity is required");
  }
}

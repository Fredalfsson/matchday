package se.matchday.backend.identity.application;

public final class CurrentUserUnavailableException extends RuntimeException {

  public CurrentUserUnavailableException() {
    super("An authenticated user identity is required");
  }
}

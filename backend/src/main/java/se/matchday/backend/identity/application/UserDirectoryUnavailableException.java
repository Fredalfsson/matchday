package se.matchday.backend.identity.application;

public final class UserDirectoryUnavailableException extends RuntimeException {

  public UserDirectoryUnavailableException() {
    super("The public user directory is unavailable");
  }
}

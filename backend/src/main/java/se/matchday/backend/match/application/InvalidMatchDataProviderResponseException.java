package se.matchday.backend.match.application;

public final class InvalidMatchDataProviderResponseException extends RuntimeException {

  public InvalidMatchDataProviderResponseException(String message) {
    super(message);
  }

  public InvalidMatchDataProviderResponseException(String message, Throwable cause) {
    super(message, cause);
  }
}

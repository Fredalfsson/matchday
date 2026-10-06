package se.matchday.backend.match.application;

public final class MatchDataProviderUnavailableException extends RuntimeException {

  public MatchDataProviderUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}

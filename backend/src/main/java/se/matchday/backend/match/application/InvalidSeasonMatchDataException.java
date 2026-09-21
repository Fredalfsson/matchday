package se.matchday.backend.match.application;

public final class InvalidSeasonMatchDataException extends RuntimeException {

  InvalidSeasonMatchDataException(String message) {
    super(message);
  }
}

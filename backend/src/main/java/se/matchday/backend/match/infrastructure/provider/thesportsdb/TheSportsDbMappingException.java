package se.matchday.backend.match.infrastructure.provider.thesportsdb;

final class TheSportsDbMappingException extends RuntimeException {

  TheSportsDbMappingException(String message) {
    super(message);
  }

  TheSportsDbMappingException(String message, Throwable cause) {
    super(message, cause);
  }
}

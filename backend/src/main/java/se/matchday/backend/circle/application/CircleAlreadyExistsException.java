package se.matchday.backend.circle.application;

import java.util.UUID;

public final class CircleAlreadyExistsException extends RuntimeException {

  public CircleAlreadyExistsException(UUID matchId, Throwable cause) {
    super("A circle already exists for match " + matchId, cause);
  }
}

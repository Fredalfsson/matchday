package se.matchday.backend.circle.application;

import java.util.UUID;

public final class CircleNotFoundByIdException extends RuntimeException {

  public CircleNotFoundByIdException(UUID circleId) {
    super("Circle " + circleId + " was not found");
  }
}

package se.matchday.backend.circle.application;

import java.util.UUID;

public final class CircleNotFoundException extends RuntimeException {

  CircleNotFoundException(UUID matchId) {
    super("A circle was not found for match " + matchId);
  }
}

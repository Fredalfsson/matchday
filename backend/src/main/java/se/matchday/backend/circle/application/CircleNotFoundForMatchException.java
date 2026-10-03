package se.matchday.backend.circle.application;

import java.util.UUID;

public final class CircleNotFoundForMatchException extends RuntimeException {

  CircleNotFoundForMatchException(UUID matchId) {
    super("A circle was not found for match " + matchId);
  }
}

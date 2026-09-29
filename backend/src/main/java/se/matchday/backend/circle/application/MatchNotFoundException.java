package se.matchday.backend.circle.application;

import java.util.UUID;

public final class MatchNotFoundException extends RuntimeException {

  MatchNotFoundException(UUID matchId) {
    super("Match " + matchId + " was not found");
  }
}

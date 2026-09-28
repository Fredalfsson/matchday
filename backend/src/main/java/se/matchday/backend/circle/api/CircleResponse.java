package se.matchday.backend.circle.api;

import java.time.Instant;
import java.util.UUID;
import se.matchday.backend.circle.application.CircleCreationResult;

record CircleResponse(UUID id, UUID matchId, Instant createdAt, boolean membershipActive) {

  static CircleResponse from(CircleCreationResult result) {
    return new CircleResponse(result.id(), result.matchId(), result.createdAt(), true);
  }
}

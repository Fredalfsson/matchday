package se.matchday.backend.circle.api;

import java.time.Instant;
import java.util.UUID;
import se.matchday.backend.circle.domain.Circle;

record CircleResponse(UUID id, UUID matchId, Instant createdAt, boolean membershipActive) {

  static CircleResponse fromCreatedCircle(Circle circle) {
    return new CircleResponse(circle.id(), circle.matchId(), circle.createdAt(), true);
  }
}

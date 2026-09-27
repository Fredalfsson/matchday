package se.matchday.backend.circle.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Circle(UUID id, UUID matchId, UUID createdByUserId, Instant createdAt) {

  public Circle {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(matchId, "matchId must not be null");
    Objects.requireNonNull(createdByUserId, "createdByUserId must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}

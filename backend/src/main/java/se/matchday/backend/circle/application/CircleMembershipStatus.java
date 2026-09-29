package se.matchday.backend.circle.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CircleMembershipStatus(
    UUID id, UUID matchId, Instant createdAt, boolean membershipActive) {

  public CircleMembershipStatus {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(matchId, "matchId must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}

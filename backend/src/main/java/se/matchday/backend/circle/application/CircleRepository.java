package se.matchday.backend.circle.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import se.matchday.backend.circle.domain.Circle;

public interface CircleRepository {

  Circle createWithCreatorMembership(UUID matchId, UUID creatorUserId, Instant createdAt);

  boolean existsById(UUID circleId);

  Optional<Circle> findByMatchId(UUID matchId);

  boolean hasActiveMembership(UUID circleId, UUID userId);

  void addMembershipIfAbsent(UUID circleId, UUID userId, Instant joinedAt);

  void removeMembershipIfPresent(UUID circleId, UUID userId);
}

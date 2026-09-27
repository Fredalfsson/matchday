package se.matchday.backend.circle.application;

import java.time.Instant;
import java.util.UUID;
import se.matchday.backend.circle.domain.Circle;

public interface CircleRepository {

  Circle createWithCreatorMembership(UUID matchId, UUID creatorUserId, Instant createdAt);
}

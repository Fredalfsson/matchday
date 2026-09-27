package se.matchday.backend.circle.infrastructure.persistence.jpa;

import java.util.UUID;

record CircleMembershipJpaId(UUID circleId, UUID userId) {}

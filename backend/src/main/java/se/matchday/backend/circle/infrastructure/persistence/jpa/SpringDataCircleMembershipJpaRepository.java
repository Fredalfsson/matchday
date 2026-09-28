package se.matchday.backend.circle.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCircleMembershipJpaRepository
    extends JpaRepository<CircleMembershipJpaEntity, CircleMembershipJpaId> {

  boolean existsByCircleIdAndUserId(UUID circleId, UUID userId);
}

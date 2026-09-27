package se.matchday.backend.circle.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(CircleMembershipJpaId.class)
@Table(name = "circle_memberships")
class CircleMembershipJpaEntity {

  @Id
  @Column(name = "circle_id", nullable = false, updatable = false)
  private UUID circleId = new UUID(0, 0);

  @Id
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId = new UUID(0, 0);

  @Column(name = "joined_at", nullable = false, updatable = false)
  private Instant joinedAt = Instant.EPOCH;

  protected CircleMembershipJpaEntity() {}

  CircleMembershipJpaEntity(UUID circleId, UUID userId, Instant joinedAt) {
    this.circleId = circleId;
    this.userId = userId;
    this.joinedAt = joinedAt;
  }
}

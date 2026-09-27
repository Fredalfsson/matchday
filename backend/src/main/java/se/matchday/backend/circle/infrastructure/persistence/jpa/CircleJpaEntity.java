package se.matchday.backend.circle.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import se.matchday.backend.circle.domain.Circle;

@Entity
@Table(name = "circles")
class CircleJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private @Nullable UUID id;

  @Column(name = "match_id", nullable = false, updatable = false, unique = true)
  private UUID matchId = new UUID(0, 0);

  @Column(name = "created_by_user_id", nullable = false, updatable = false)
  private UUID createdByUserId = new UUID(0, 0);

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.EPOCH;

  protected CircleJpaEntity() {}

  CircleJpaEntity(UUID matchId, UUID createdByUserId, Instant createdAt) {
    this.matchId = matchId;
    this.createdByUserId = createdByUserId;
    this.createdAt = createdAt;
  }

  UUID id() {
    if (id == null) {
      throw new IllegalStateException("A persisted circle must have an id");
    }
    return id;
  }

  Circle toDomain() {
    return new Circle(id(), matchId, createdByUserId, createdAt);
  }
}

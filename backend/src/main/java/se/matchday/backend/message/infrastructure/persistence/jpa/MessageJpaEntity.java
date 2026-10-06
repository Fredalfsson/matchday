package se.matchday.backend.message.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import se.matchday.backend.message.domain.Message;

@Entity
@Table(name = "messages")
class MessageJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private @Nullable UUID id;

  @Column(name = "circle_id", nullable = false, updatable = false)
  private UUID circleId = new UUID(0, 0);

  @Column(name = "author_user_id", nullable = false, updatable = false)
  private UUID authorUserId = new UUID(0, 0);

  @Column(nullable = false, updatable = false, length = 1_000)
  private String content = "";

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.EPOCH;

  protected MessageJpaEntity() {}

  Message toDomain() {
    if (id == null) {
      throw new IllegalStateException("A persisted message must have an id");
    }
    return new Message(id, circleId, authorUserId, content, createdAt);
  }
}

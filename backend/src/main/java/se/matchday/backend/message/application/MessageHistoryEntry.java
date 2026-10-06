package se.matchday.backend.message.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MessageHistoryEntry(
    UUID id, UUID circleId, String content, String authorUsername, Instant createdAt) {

  public MessageHistoryEntry {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(circleId, "circleId must not be null");
    Objects.requireNonNull(content, "content must not be null");
    Objects.requireNonNull(authorUsername, "authorUsername must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }
}

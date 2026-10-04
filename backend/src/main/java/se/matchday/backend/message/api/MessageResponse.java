package se.matchday.backend.message.api;

import java.time.Instant;
import java.util.UUID;
import se.matchday.backend.message.application.MessageCreationResult;

record MessageResponse(
    UUID id, UUID circleId, String content, String authorUsername, Instant createdAt) {

  static MessageResponse from(MessageCreationResult result) {
    return new MessageResponse(
        result.id(),
        result.circleId(),
        result.content(),
        result.authorUsername(),
        result.createdAt());
  }
}

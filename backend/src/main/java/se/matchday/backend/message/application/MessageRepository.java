package se.matchday.backend.message.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import se.matchday.backend.message.domain.Message;

public interface MessageRepository {

  Optional<Message> createIfActiveMember(
      UUID circleId, UUID authorUserId, String content, Instant createdAt);
}

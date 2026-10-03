package se.matchday.backend.message.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.matchday.backend.message.application.MessageRepository;
import se.matchday.backend.message.domain.Message;

@Repository
class JpaMessageRepositoryAdapter implements MessageRepository {

  private final SpringDataMessageJpaRepository messageRepository;

  JpaMessageRepositoryAdapter(SpringDataMessageJpaRepository messageRepository) {
    this.messageRepository = messageRepository;
  }

  @Override
  @Transactional
  public Optional<Message> createIfActiveMember(
      UUID circleId, UUID authorUserId, String content, Instant createdAt) {
    Message message = new Message(UUID.randomUUID(), circleId, authorUserId, content, createdAt);
    int inserted =
        messageRepository.insertIfActiveMember(
            message.id(),
            message.circleId(),
            message.authorUserId(),
            message.content(),
            message.createdAt());
    return inserted == 1 ? Optional.of(message) : Optional.empty();
  }
}

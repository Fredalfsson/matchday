package se.matchday.backend.message.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.matchday.backend.message.application.MessagePage;
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

  @Override
  @Transactional
  public Optional<MessagePage> findPageForActiveMember(
      UUID circleId, UUID userId, int page, int size) {
    if (messageRepository.lockActiveMembership(circleId, userId).isEmpty()) {
      return Optional.empty();
    }

    Slice<MessageJpaEntity> entities =
        messageRepository.findByCircleIdOrderByCreatedAtDescIdDesc(
            circleId, PageRequest.of(page, size));
    List<Message> messages =
        entities.getContent().stream().map(entity -> entity.toDomain()).toList();
    return Optional.of(new MessagePage(messages, entities.hasNext()));
  }
}

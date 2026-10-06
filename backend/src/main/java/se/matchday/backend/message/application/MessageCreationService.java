package se.matchday.backend.message.application;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import se.matchday.backend.circle.application.ActiveCircleMembershipRequiredException;
import se.matchday.backend.circle.application.CircleNotFoundByIdException;
import se.matchday.backend.circle.application.CircleRepository;
import se.matchday.backend.identity.application.CurrentUser;
import se.matchday.backend.identity.application.CurrentUserUnavailableException;
import se.matchday.backend.identity.application.UserDirectory;
import se.matchday.backend.identity.application.UserDirectoryUnavailableException;
import se.matchday.backend.message.domain.Message;

public final class MessageCreationService {

  private final CurrentUser currentUser;
  private final UserDirectory userDirectory;
  private final CircleRepository circleRepository;
  private final MessageRepository messageRepository;
  private final Clock clock;

  public MessageCreationService(
      CurrentUser currentUser,
      UserDirectory userDirectory,
      CircleRepository circleRepository,
      MessageRepository messageRepository,
      Clock clock) {
    this.currentUser = currentUser;
    this.userDirectory = userDirectory;
    this.circleRepository = circleRepository;
    this.messageRepository = messageRepository;
    this.clock = clock;
  }

  public MessageCreationResult createForCircle(UUID circleId, String content) {
    Objects.requireNonNull(circleId, "circleId must not be null");
    UUID userId = currentUser.userId().orElseThrow(CurrentUserUnavailableException::new);

    if (!circleRepository.existsById(circleId)) {
      throw new CircleNotFoundByIdException(circleId);
    }
    if (!circleRepository.hasActiveMembership(circleId, userId)) {
      throw new ActiveCircleMembershipRequiredException(circleId);
    }

    String normalizedContent = normalizeContent(content);
    String authorUsername = findAuthorUsername(userId);
    Message message =
        messageRepository
            .createIfActiveMember(circleId, userId, normalizedContent, clock.instant())
            .orElseThrow(() -> new ActiveCircleMembershipRequiredException(circleId));

    return new MessageCreationResult(
        message.id(), message.circleId(), message.content(), authorUsername, message.createdAt());
  }

  private String findAuthorUsername(UUID userId) {
    Map<UUID, String> usernames = userDirectory.findUsernamesByUserIds(Set.of(userId));
    String username = usernames.get(userId);
    if (username == null || username.isBlank()) {
      throw new UserDirectoryUnavailableException();
    }
    return username;
  }

  private static String normalizeContent(String content) {
    try {
      return Message.normalizeContent(content);
    } catch (IllegalArgumentException exception) {
      throw new InvalidMessageContentException(exception);
    }
  }
}

package se.matchday.backend.message.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import se.matchday.backend.circle.application.ActiveCircleMembershipRequiredException;
import se.matchday.backend.circle.application.CircleNotFoundByIdException;
import se.matchday.backend.circle.application.CircleRepository;
import se.matchday.backend.identity.application.CurrentUser;
import se.matchday.backend.identity.application.CurrentUserUnavailableException;
import se.matchday.backend.identity.application.UserDirectory;
import se.matchday.backend.identity.application.UserDirectoryUnavailableException;
import se.matchday.backend.message.domain.Message;

public final class MessageHistoryService {

  private static final int MAXIMUM_PAGE_SIZE = 100;

  private final CurrentUser currentUser;
  private final UserDirectory userDirectory;
  private final CircleRepository circleRepository;
  private final MessageRepository messageRepository;

  public MessageHistoryService(
      CurrentUser currentUser,
      UserDirectory userDirectory,
      CircleRepository circleRepository,
      MessageRepository messageRepository) {
    this.currentUser = currentUser;
    this.userDirectory = userDirectory;
    this.circleRepository = circleRepository;
    this.messageRepository = messageRepository;
  }

  public MessageHistoryResult listForCircle(UUID circleId, int page, int size) {
    Objects.requireNonNull(circleId, "circleId must not be null");
    UUID userId = currentUser.userId().orElseThrow(CurrentUserUnavailableException::new);
    validatePagination(page, size);

    if (!circleRepository.existsById(circleId)) {
      throw new CircleNotFoundByIdException(circleId);
    }

    MessagePage messagePage =
        messageRepository
            .findPageForActiveMember(circleId, userId, page, size)
            .orElseThrow(() -> new ActiveCircleMembershipRequiredException(circleId));
    if (messagePage.messages().isEmpty()) {
      return new MessageHistoryResult(List.of(), page, size, messagePage.hasNext());
    }

    Map<UUID, String> usernames = findUsernames(messagePage.messages());
    List<MessageHistoryEntry> entries =
        messagePage.messages().stream().map(message -> toHistoryEntry(message, usernames)).toList();
    return new MessageHistoryResult(entries, page, size, messagePage.hasNext());
  }

  private Map<UUID, String> findUsernames(List<Message> messages) {
    Set<UUID> authorUserIds =
        messages.stream()
            .map(message -> message.authorUserId())
            .collect(Collectors.toUnmodifiableSet());
    return userDirectory.findUsernamesByUserIds(authorUserIds);
  }

  private static MessageHistoryEntry toHistoryEntry(Message message, Map<UUID, String> usernames) {
    String username = usernames.get(message.authorUserId());
    if (username == null || username.isBlank()) {
      throw new UserDirectoryUnavailableException();
    }
    return new MessageHistoryEntry(
        message.id(), message.circleId(), message.content(), username, message.createdAt());
  }

  private static void validatePagination(int page, int size) {
    if (page < 0) {
      throw new InvalidMessagePaginationException("page must be zero or greater");
    }
    if (size < 1 || size > MAXIMUM_PAGE_SIZE) {
      throw new InvalidMessagePaginationException("size must be between 1 and 100");
    }
  }
}

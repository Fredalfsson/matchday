package se.matchday.backend.message.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import se.matchday.backend.circle.application.ActiveCircleMembershipRequiredException;
import se.matchday.backend.circle.application.CircleNotFoundByIdException;
import se.matchday.backend.circle.application.CircleRepository;
import se.matchday.backend.identity.application.CurrentUser;
import se.matchday.backend.identity.application.CurrentUserUnavailableException;
import se.matchday.backend.identity.application.UserDirectory;
import se.matchday.backend.identity.application.UserDirectoryUnavailableException;
import se.matchday.backend.message.domain.Message;

class MessageHistoryServiceTest {

  private static final UUID CIRCLE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
  private static final UUID CURRENT_USER_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID SECOND_AUTHOR_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000002");
  private static final UUID FIRST_MESSAGE_ID =
      UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID SECOND_MESSAGE_ID =
      UUID.fromString("30000000-0000-0000-0000-000000000002");
  private static final Instant OLDER_CREATED_AT = Instant.parse("2026-10-03T10:15:30Z");
  private static final Instant NEWER_CREATED_AT = Instant.parse("2026-10-03T10:16:30Z");

  private final CurrentUser currentUser = () -> Optional.of(CURRENT_USER_ID);
  private final UserDirectory userDirectory = mock(UserDirectory.class);
  private final CircleRepository circleRepository = mock(CircleRepository.class);
  private final MessageRepository messageRepository = mock(MessageRepository.class);

  @Test
  void returnsAMessagePageWithPublicAuthorNames() {
    Message first =
        new Message(
            FIRST_MESSAGE_ID, CIRCLE_ID, CURRENT_USER_ID, "Newest message", NEWER_CREATED_AT);
    Message second =
        new Message(
            SECOND_MESSAGE_ID, CIRCLE_ID, SECOND_AUTHOR_ID, "Older message", OLDER_CREATED_AT);
    when(circleRepository.existsById(CIRCLE_ID)).thenReturn(true);
    when(messageRepository.findPageForActiveMember(CIRCLE_ID, CURRENT_USER_ID, 1, 2))
        .thenReturn(Optional.of(new MessagePage(List.of(first, second), true)));
    when(userDirectory.findUsernamesByUserIds(Set.of(CURRENT_USER_ID, SECOND_AUTHOR_ID)))
        .thenReturn(Map.of(CURRENT_USER_ID, "sara", SECOND_AUTHOR_ID, "alex"));

    MessageHistoryResult result = service(currentUser).listForCircle(CIRCLE_ID, 1, 2);

    assertThat(result)
        .isEqualTo(
            new MessageHistoryResult(
                List.of(
                    new MessageHistoryEntry(
                        FIRST_MESSAGE_ID, CIRCLE_ID, "Newest message", "sara", NEWER_CREATED_AT),
                    new MessageHistoryEntry(
                        SECOND_MESSAGE_ID, CIRCLE_ID, "Older message", "alex", OLDER_CREATED_AT)),
                1,
                2,
                true));
    verify(userDirectory).findUsernamesByUserIds(Set.of(CURRENT_USER_ID, SECOND_AUTHOR_ID));
  }

  @Test
  void returnsAnEmptyPageWithoutLookingUpUsernames() {
    when(circleRepository.existsById(CIRCLE_ID)).thenReturn(true);
    when(messageRepository.findPageForActiveMember(CIRCLE_ID, CURRENT_USER_ID, 0, 50))
        .thenReturn(Optional.of(new MessagePage(List.of(), false)));

    MessageHistoryResult result = service(currentUser).listForCircle(CIRCLE_ID, 0, 50);

    assertThat(result).isEqualTo(new MessageHistoryResult(List.of(), 0, 50, false));
    verifyNoInteractions(userDirectory);
  }

  @Test
  void rejectsHistoryWhenNoCurrentUserIdentityIsAvailableBeforeValidatingPagination() {
    MessageHistoryService service = service(Optional::empty);

    assertThatThrownBy(() -> service.listForCircle(CIRCLE_ID, -1, 50))
        .isInstanceOf(CurrentUserUnavailableException.class)
        .hasMessage("An authenticated user identity is required");
    verifyNoInteractions(userDirectory, circleRepository, messageRepository);
  }

  @Test
  void rejectsHistoryForAnUnknownCircle() {
    when(circleRepository.existsById(CIRCLE_ID)).thenReturn(false);

    assertThatThrownBy(() -> service(currentUser).listForCircle(CIRCLE_ID, 0, 50))
        .isInstanceOf(CircleNotFoundByIdException.class)
        .hasMessage("Circle " + CIRCLE_ID + " was not found");
    verifyNoInteractions(userDirectory, messageRepository);
  }

  @Test
  void rejectsHistoryForAUserWithoutActiveMembership() {
    when(circleRepository.existsById(CIRCLE_ID)).thenReturn(true);
    when(messageRepository.findPageForActiveMember(CIRCLE_ID, CURRENT_USER_ID, 0, 50))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service(currentUser).listForCircle(CIRCLE_ID, 0, 50))
        .isInstanceOf(ActiveCircleMembershipRequiredException.class)
        .hasMessage("An active membership is required for circle " + CIRCLE_ID);
    verifyNoInteractions(userDirectory);
  }

  @Test
  void rejectsANegativePageBeforeAccessingRepositories() {
    assertThatThrownBy(() -> service(currentUser).listForCircle(CIRCLE_ID, -1, 50))
        .isInstanceOf(InvalidMessagePaginationException.class)
        .hasMessage("page must be zero or greater");
    verifyNoInteractions(userDirectory, circleRepository, messageRepository);
  }

  @Test
  void rejectsAPageSizeBelowTheAllowedRangeBeforeAccessingRepositories() {
    assertThatThrownBy(() -> service(currentUser).listForCircle(CIRCLE_ID, 0, 0))
        .isInstanceOf(InvalidMessagePaginationException.class)
        .hasMessage("size must be between 1 and 100");
    verifyNoInteractions(userDirectory, circleRepository, messageRepository);
  }

  @Test
  void rejectsAPageSizeAboveTheAllowedRangeBeforeAccessingRepositories() {
    assertThatThrownBy(() -> service(currentUser).listForCircle(CIRCLE_ID, 0, 101))
        .isInstanceOf(InvalidMessagePaginationException.class)
        .hasMessage("size must be between 1 and 100");
    verifyNoInteractions(userDirectory, circleRepository, messageRepository);
  }

  @Test
  void propagatesAnUnavailableUserDirectory() {
    Message message =
        new Message(FIRST_MESSAGE_ID, CIRCLE_ID, CURRENT_USER_ID, "Message", NEWER_CREATED_AT);
    allowHistory(message);
    when(userDirectory.findUsernamesByUserIds(Set.of(CURRENT_USER_ID)))
        .thenThrow(new UserDirectoryUnavailableException());

    assertThatThrownBy(() -> service(currentUser).listForCircle(CIRCLE_ID, 0, 50))
        .isInstanceOf(UserDirectoryUnavailableException.class)
        .hasMessage("The public user directory is unavailable");
  }

  @Test
  void treatsAnIncompleteUserDirectoryResultAsUnavailable() {
    Message message =
        new Message(FIRST_MESSAGE_ID, CIRCLE_ID, CURRENT_USER_ID, "Message", NEWER_CREATED_AT);
    allowHistory(message);
    when(userDirectory.findUsernamesByUserIds(Set.of(CURRENT_USER_ID))).thenReturn(Map.of());

    assertThatThrownBy(() -> service(currentUser).listForCircle(CIRCLE_ID, 0, 50))
        .isInstanceOf(UserDirectoryUnavailableException.class)
        .hasMessage("The public user directory is unavailable");
  }

  @Test
  void treatsABlankUsernameAsAnUnavailableUserDirectory() {
    Message message =
        new Message(FIRST_MESSAGE_ID, CIRCLE_ID, CURRENT_USER_ID, "Message", NEWER_CREATED_AT);
    allowHistory(message);
    when(userDirectory.findUsernamesByUserIds(Set.of(CURRENT_USER_ID)))
        .thenReturn(Map.of(CURRENT_USER_ID, " \n\t "));

    assertThatThrownBy(() -> service(currentUser).listForCircle(CIRCLE_ID, 0, 50))
        .isInstanceOf(UserDirectoryUnavailableException.class)
        .hasMessage("The public user directory is unavailable");
  }

  private void allowHistory(Message message) {
    when(circleRepository.existsById(CIRCLE_ID)).thenReturn(true);
    when(messageRepository.findPageForActiveMember(CIRCLE_ID, CURRENT_USER_ID, 0, 50))
        .thenReturn(Optional.of(new MessagePage(List.of(message), false)));
  }

  private MessageHistoryService service(CurrentUser user) {
    return new MessageHistoryService(user, userDirectory, circleRepository, messageRepository);
  }
}

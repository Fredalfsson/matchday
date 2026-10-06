package se.matchday.backend.message.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

class MessageCreationServiceTest {

  private static final UUID MESSAGE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID CIRCLE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final String USERNAME = "sara";
  private static final Instant NOW = Instant.parse("2026-10-03T10:15:30Z");

  private final CurrentUser currentUser = () -> Optional.of(USER_ID);
  private final UserDirectory userDirectory = mock(UserDirectory.class);
  private final CircleRepository circleRepository = mock(CircleRepository.class);
  private final MessageRepository messageRepository = mock(MessageRepository.class);

  @Test
  void createsAMessageForAnActiveMember() {
    String submittedContent = "  First line\nSecond line  ";
    String normalizedContent = "First line\nSecond line";
    Message message = new Message(MESSAGE_ID, CIRCLE_ID, USER_ID, normalizedContent, NOW);
    allowActiveMembership();
    when(userDirectory.findUsernamesByUserIds(Set.of(USER_ID)))
        .thenReturn(Map.of(USER_ID, USERNAME));
    when(messageRepository.createIfActiveMember(CIRCLE_ID, USER_ID, normalizedContent, NOW))
        .thenReturn(Optional.of(message));

    MessageCreationResult result =
        service(currentUser).createForCircle(CIRCLE_ID, submittedContent);

    assertThat(result)
        .isEqualTo(
            new MessageCreationResult(MESSAGE_ID, CIRCLE_ID, normalizedContent, USERNAME, NOW));
    verify(messageRepository).createIfActiveMember(CIRCLE_ID, USER_ID, normalizedContent, NOW);
  }

  @Test
  void rejectsCreationWhenNoCurrentUserIdentityIsAvailable() {
    MessageCreationService service = service(Optional::empty);

    assertThatThrownBy(() -> service.createForCircle(CIRCLE_ID, "message"))
        .isInstanceOf(CurrentUserUnavailableException.class)
        .hasMessage("An authenticated user identity is required");
    verifyNoInteractions(userDirectory, circleRepository, messageRepository);
  }

  @Test
  void rejectsCreationForAnUnknownCircle() {
    when(circleRepository.existsById(CIRCLE_ID)).thenReturn(false);

    assertThatThrownBy(() -> service(currentUser).createForCircle(CIRCLE_ID, "message"))
        .isInstanceOf(CircleNotFoundByIdException.class)
        .hasMessage("Circle " + CIRCLE_ID + " was not found");
    verifyNoInteractions(userDirectory, messageRepository);
  }

  @Test
  void rejectsCreationForAUserWithoutActiveMembership() {
    when(circleRepository.existsById(CIRCLE_ID)).thenReturn(true);
    when(circleRepository.hasActiveMembership(CIRCLE_ID, USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> service(currentUser).createForCircle(CIRCLE_ID, "message"))
        .isInstanceOf(ActiveCircleMembershipRequiredException.class)
        .hasMessage("An active membership is required for circle " + CIRCLE_ID);
    verifyNoInteractions(userDirectory, messageRepository);
  }

  @Test
  void validatesNormalizedContentBeforeLookingUpTheAuthor() {
    allowActiveMembership();

    assertThatThrownBy(() -> service(currentUser).createForCircle(CIRCLE_ID, " \n\t "))
        .isInstanceOf(InvalidMessageContentException.class)
        .hasMessage("content must not be blank");
    verifyNoInteractions(userDirectory, messageRepository);
  }

  @Test
  void doesNotPersistWhenTheUserDirectoryIsUnavailable() {
    allowActiveMembership();
    when(userDirectory.findUsernamesByUserIds(Set.of(USER_ID)))
        .thenThrow(new UserDirectoryUnavailableException());

    assertThatThrownBy(() -> service(currentUser).createForCircle(CIRCLE_ID, "message"))
        .isInstanceOf(UserDirectoryUnavailableException.class)
        .hasMessage("The public user directory is unavailable");
    verifyNoInteractions(messageRepository);
  }

  @Test
  void treatsAnIncompleteUserDirectoryResultAsUnavailable() {
    allowActiveMembership();
    when(userDirectory.findUsernamesByUserIds(Set.of(USER_ID))).thenReturn(Map.of());

    assertThatThrownBy(() -> service(currentUser).createForCircle(CIRCLE_ID, "message"))
        .isInstanceOf(UserDirectoryUnavailableException.class)
        .hasMessage("The public user directory is unavailable");
    verifyNoInteractions(messageRepository);
  }

  @Test
  void treatsABlankUsernameAsAnUnavailableUserDirectory() {
    allowActiveMembership();
    when(userDirectory.findUsernamesByUserIds(Set.of(USER_ID)))
        .thenReturn(Map.of(USER_ID, " \n\t "));

    assertThatThrownBy(() -> service(currentUser).createForCircle(CIRCLE_ID, "message"))
        .isInstanceOf(UserDirectoryUnavailableException.class)
        .hasMessage("The public user directory is unavailable");
    verifyNoInteractions(messageRepository);
  }

  @Test
  void rejectsCreationWhenMembershipIsRemovedBeforeTheAtomicInsert() {
    allowActiveMembership();
    when(userDirectory.findUsernamesByUserIds(Set.of(USER_ID)))
        .thenReturn(Map.of(USER_ID, USERNAME));
    when(messageRepository.createIfActiveMember(CIRCLE_ID, USER_ID, "message", NOW))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service(currentUser).createForCircle(CIRCLE_ID, "message"))
        .isInstanceOf(ActiveCircleMembershipRequiredException.class)
        .hasMessage("An active membership is required for circle " + CIRCLE_ID);
  }

  private void allowActiveMembership() {
    when(circleRepository.existsById(CIRCLE_ID)).thenReturn(true);
    when(circleRepository.hasActiveMembership(CIRCLE_ID, USER_ID)).thenReturn(true);
  }

  private MessageCreationService service(CurrentUser user) {
    return new MessageCreationService(
        user, userDirectory, circleRepository, messageRepository, Clock.fixed(NOW, ZoneOffset.UTC));
  }
}

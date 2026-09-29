package se.matchday.backend.circle.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import se.matchday.backend.circle.domain.Circle;
import se.matchday.backend.identity.application.CurrentUser;
import se.matchday.backend.match.application.MatchRepository;

class CircleMembershipServiceTest {

  private static final UUID MATCH_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID CREATOR_USER_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000002");
  private static final UUID CIRCLE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final Instant NOW = Instant.parse("2026-09-29T10:15:30Z");

  private final MatchRepository matchRepository = mock(MatchRepository.class);
  private final CircleRepository circleRepository = mock(CircleRepository.class);

  @Test
  void joinsTheCurrentUserToAnExistingCircle() {
    CurrentUser currentUser = () -> Optional.of(USER_ID);
    Circle circle = new Circle(CIRCLE_ID, MATCH_ID, CREATOR_USER_ID, NOW.minusSeconds(60));
    when(matchRepository.existsById(MATCH_ID)).thenReturn(true);
    when(circleRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.of(circle));
    CircleMembershipService service = service(currentUser);

    service.joinForMatch(MATCH_ID);

    verify(matchRepository).existsById(MATCH_ID);
    verify(circleRepository).findByMatchId(MATCH_ID);
    verify(circleRepository).addMembershipIfAbsent(CIRCLE_ID, USER_ID, NOW);
  }

  @Test
  void rejectsJoinWhenNoCurrentUserIdentityIsAvailable() {
    CircleMembershipService service = service(Optional::empty);

    assertThatThrownBy(() -> service.joinForMatch(MATCH_ID))
        .isInstanceOf(CurrentUserUnavailableException.class)
        .hasMessage("An authenticated user identity is required");
    verifyNoInteractions(matchRepository, circleRepository);
  }

  @Test
  void rejectsJoinForAnUnknownMatch() {
    CurrentUser currentUser = () -> Optional.of(USER_ID);
    when(matchRepository.existsById(MATCH_ID)).thenReturn(false);
    CircleMembershipService service = service(currentUser);

    assertThatThrownBy(() -> service.joinForMatch(MATCH_ID))
        .isInstanceOf(MatchNotFoundException.class)
        .hasMessage("Match " + MATCH_ID + " was not found");
    verifyNoInteractions(circleRepository);
  }

  @Test
  void reportsWhenAnExistingMatchHasNoCircle() {
    CurrentUser currentUser = () -> Optional.of(USER_ID);
    when(matchRepository.existsById(MATCH_ID)).thenReturn(true);
    when(circleRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.empty());
    CircleMembershipService service = service(currentUser);

    assertThatThrownBy(() -> service.joinForMatch(MATCH_ID))
        .isInstanceOf(CircleNotFoundException.class)
        .hasMessage("A circle was not found for match " + MATCH_ID);
    verify(circleRepository).findByMatchId(MATCH_ID);
  }

  private CircleMembershipService service(CurrentUser currentUser) {
    return new CircleMembershipService(
        currentUser, matchRepository, circleRepository, Clock.fixed(NOW, ZoneOffset.UTC));
  }
}

package se.matchday.backend.circle.application;

import static org.assertj.core.api.Assertions.assertThat;
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

class CircleCreationServiceTest {

  private static final UUID MATCH_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID CIRCLE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final Instant NOW = Instant.parse("2026-09-27T10:15:30Z");

  private final MatchRepository matchRepository = mock(MatchRepository.class);
  private final CircleRepository circleRepository = mock(CircleRepository.class);

  @Test
  void createsACircleAndCreatorMembershipForAnExistingMatch() {
    CurrentUser currentUser = () -> Optional.of(USER_ID);
    Circle createdCircle = new Circle(CIRCLE_ID, MATCH_ID, USER_ID, NOW);
    when(matchRepository.existsById(MATCH_ID)).thenReturn(true);
    when(circleRepository.createWithCreatorMembership(MATCH_ID, USER_ID, NOW))
        .thenReturn(createdCircle);
    CircleCreationService service = service(currentUser);

    CircleCreationResult result = service.createForMatch(MATCH_ID);

    assertThat(result).isEqualTo(new CircleCreationResult(CIRCLE_ID, MATCH_ID, NOW));
    verify(matchRepository).existsById(MATCH_ID);
    verify(circleRepository).createWithCreatorMembership(MATCH_ID, USER_ID, NOW);
  }

  @Test
  void rejectsCreationWhenNoCurrentUserIdentityIsAvailable() {
    CircleCreationService service = service(Optional::empty);

    assertThatThrownBy(() -> service.createForMatch(MATCH_ID))
        .isInstanceOf(CurrentUserUnavailableException.class)
        .hasMessage("An authenticated user identity is required");
    verifyNoInteractions(matchRepository, circleRepository);
  }

  @Test
  void rejectsCreationForAnUnknownMatch() {
    CurrentUser currentUser = () -> Optional.of(USER_ID);
    when(matchRepository.existsById(MATCH_ID)).thenReturn(false);
    CircleCreationService service = service(currentUser);

    assertThatThrownBy(() -> service.createForMatch(MATCH_ID))
        .isInstanceOf(MatchNotFoundException.class)
        .hasMessage("Match " + MATCH_ID + " was not found");
    verifyNoInteractions(circleRepository);
  }

  private CircleCreationService service(CurrentUser currentUser) {
    return new CircleCreationService(
        currentUser, matchRepository, circleRepository, Clock.fixed(NOW, ZoneOffset.UTC));
  }
}

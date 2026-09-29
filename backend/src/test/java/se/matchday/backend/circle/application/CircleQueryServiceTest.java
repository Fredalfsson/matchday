package se.matchday.backend.circle.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import se.matchday.backend.circle.domain.Circle;
import se.matchday.backend.identity.application.CurrentUser;
import se.matchday.backend.match.application.MatchRepository;

class CircleQueryServiceTest {

  private static final UUID MATCH_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID CIRCLE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final Instant CREATED_AT = Instant.parse("2026-09-28T10:15:30Z");

  private final MatchRepository matchRepository = mock(MatchRepository.class);
  private final CircleRepository circleRepository = mock(CircleRepository.class);

  @Test
  void returnsCircleWithActiveMembershipForAMember() {
    Circle circle = circle();
    when(matchRepository.existsById(MATCH_ID)).thenReturn(true);
    when(circleRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.of(circle));
    when(circleRepository.hasActiveMembership(CIRCLE_ID, USER_ID)).thenReturn(true);

    CircleMembershipStatus result = service(() -> Optional.of(USER_ID)).findForMatch(MATCH_ID);

    assertThat(result).isEqualTo(new CircleMembershipStatus(CIRCLE_ID, MATCH_ID, CREATED_AT, true));
  }

  @Test
  void returnsCircleWithInactiveMembershipForANonMember() {
    Circle circle = circle();
    when(matchRepository.existsById(MATCH_ID)).thenReturn(true);
    when(circleRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.of(circle));
    when(circleRepository.hasActiveMembership(CIRCLE_ID, USER_ID)).thenReturn(false);

    CircleMembershipStatus result = service(() -> Optional.of(USER_ID)).findForMatch(MATCH_ID);

    assertThat(result)
        .isEqualTo(new CircleMembershipStatus(CIRCLE_ID, MATCH_ID, CREATED_AT, false));
  }

  @Test
  void rejectsLookupWhenNoCurrentUserIdentityIsAvailable() {
    CircleQueryService service = service(Optional::empty);

    assertThatThrownBy(() -> service.findForMatch(MATCH_ID))
        .isInstanceOf(CurrentUserUnavailableException.class)
        .hasMessage("An authenticated user identity is required");
    verifyNoInteractions(matchRepository, circleRepository);
  }

  @Test
  void rejectsLookupForAnUnknownMatch() {
    when(matchRepository.existsById(MATCH_ID)).thenReturn(false);

    assertThatThrownBy(() -> service(() -> Optional.of(USER_ID)).findForMatch(MATCH_ID))
        .isInstanceOf(MatchNotFoundException.class)
        .hasMessage("Match " + MATCH_ID + " was not found");
    verifyNoInteractions(circleRepository);
  }

  @Test
  void rejectsLookupWhenAnExistingMatchHasNoCircle() {
    when(matchRepository.existsById(MATCH_ID)).thenReturn(true);
    when(circleRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service(() -> Optional.of(USER_ID)).findForMatch(MATCH_ID))
        .isInstanceOf(CircleNotFoundException.class)
        .hasMessage("A circle was not found for match " + MATCH_ID);
    verify(circleRepository).findByMatchId(MATCH_ID);
  }

  private CircleQueryService service(CurrentUser currentUser) {
    return new CircleQueryService(currentUser, matchRepository, circleRepository);
  }

  private Circle circle() {
    return new Circle(CIRCLE_ID, MATCH_ID, USER_ID, CREATED_AT);
  }
}

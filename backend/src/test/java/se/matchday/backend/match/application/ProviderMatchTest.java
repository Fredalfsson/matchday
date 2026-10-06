package se.matchday.backend.match.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import se.matchday.backend.match.domain.MatchStatus;

class ProviderMatchTest {

  @Test
  void acceptsTextAtThePersistenceLimitUsingUnicodeCodePoints() {
    String teamName = "😀".repeat(255);

    assertThatCode(() -> scheduledMatch(teamName, null)).doesNotThrowAnyException();
  }

  @Test
  void rejectsRequiredTextBeyondThePersistenceLimit() {
    String teamName = "😀".repeat(256);

    assertThatThrownBy(() -> scheduledMatch(teamName, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("homeTeamName must not exceed 255 characters");
  }

  @Test
  void rejectsOptionalTextBeyondThePersistenceLimit() {
    String venueName = "A".repeat(256);

    assertThatThrownBy(() -> scheduledMatch("Home", venueName))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("venueName must not exceed 255 characters");
  }

  private ProviderMatch scheduledMatch(String homeTeamName, String venueName) {
    return new ProviderMatch(
        "event-1",
        2026,
        1,
        "home-1",
        homeTeamName,
        "away-1",
        "Away",
        LocalDate.of(2026, 4, 4),
        null,
        null,
        null,
        MatchStatus.SCHEDULED,
        venueName);
  }
}

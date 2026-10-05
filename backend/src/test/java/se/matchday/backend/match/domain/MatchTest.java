package se.matchday.backend.match.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchTest {

  @Test
  void rejectsAnIncompleteScore() {
    assertThatThrownBy(() -> match(1, null, MatchStatus.FINISHED))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("both score fields must be present or absent");
  }

  @Test
  void rejectsAResultForAScheduledMatch() {
    assertThatThrownBy(() -> match(1, 0, MatchStatus.SCHEDULED))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("a scheduled match cannot have a result");
  }

  @Test
  void rejectsAFinishedMatchWithoutAResult() {
    assertThatThrownBy(() -> match(null, null, MatchStatus.FINISHED))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("a finished match must have a result");
  }

  @Test
  void rejectsInvalidIdentityAndScheduleValues() {
    assertThatThrownBy(
            () ->
                new Match(
                    UUID.randomUUID(),
                    2026,
                    1,
                    " ",
                    "Away",
                    LocalDate.of(2026, 4, 4),
                    Instant.parse("2026-04-04T13:00:00Z"),
                    null,
                    null,
                    MatchStatus.SCHEDULED,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("homeTeamName must not be blank");
  }

  private Match match(Integer homeScore, Integer awayScore, MatchStatus status) {
    return new Match(
        UUID.randomUUID(),
        2026,
        1,
        "Home",
        "Away",
        LocalDate.of(2026, 4, 4),
        Instant.parse("2026-04-04T13:00:00Z"),
        homeScore,
        awayScore,
        status,
        null);
  }
}

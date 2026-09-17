package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import se.matchday.backend.match.domain.Match;
import se.matchday.backend.match.domain.MatchStatus;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventDto;

class TheSportsDbEventMapperTest {

  private final TheSportsDbEventMapper mapper = new TheSportsDbEventMapper();

  @Test
  void mapsAFinishedMatchToTheDomainModel() {
    Match match = mapper.toMatch(finishedEvent());

    assertThat(match)
        .isEqualTo(
            new Match(
                "2398752",
                2026,
                1,
                "134728",
                "Degerfors",
                "134724",
                "Sirius",
                LocalDate.of(2026, 4, 4),
                LocalTime.of(13, 0),
                0,
                3,
                MatchStatus.FINISHED,
                "Stora Valla"));
  }

  @Test
  void mapsAnUnscheduledKickoffWithoutScoresOrVenue() {
    TheSportsDbEventDto event =
        new TheSportsDbEventDto(
            "future-1",
            "2026",
            "2",
            "134011",
            "AIK",
            "134167",
            "Halmstad",
            "2026-04-12",
            null,
            null,
            null,
            null,
            "NS",
            "no",
            null,
            null);

    Match match = mapper.toMatch(event);

    assertThat(match.kickoffTime()).isNull();
    assertThat(match.homeScore()).isNull();
    assertThat(match.awayScore()).isNull();
    assertThat(match.status()).isEqualTo(MatchStatus.SCHEDULED);
    assertThat(match.venueName()).isNull();
  }

  @Test
  void mapsAPostponedMatchBeforeOtherStatuses() {
    TheSportsDbEventDto event =
        new TheSportsDbEventDto(
            "postponed-1",
            "2026",
            "3",
            "134011",
            "AIK",
            "134167",
            "Halmstad",
            "2026-04-19",
            "15:00:00",
            null,
            null,
            null,
            "NS",
            "yes",
            null,
            "Nationalarenan");

    assertThat(mapper.toMatch(event).status()).isEqualTo(MatchStatus.POSTPONED);
  }

  @Test
  void rejectsAnEventWithoutAnExternalId() {
    TheSportsDbEventDto event =
        new TheSportsDbEventDto(
            null,
            "2026",
            "1",
            "134728",
            "Degerfors",
            "134724",
            "Sirius",
            "2026-04-04",
            "13:00:00",
            null,
            "0",
            "3",
            "FT",
            "no",
            "17256",
            "Stora Valla");

    assertThatThrownBy(() -> mapper.toMatch(event))
        .isInstanceOf(TheSportsDbMappingException.class)
        .hasMessage("Missing required field idEvent");
  }

  @Test
  void rejectsAnIncompleteScore() {
    TheSportsDbEventDto event =
        new TheSportsDbEventDto(
            "2398752",
            "2026",
            "1",
            "134728",
            "Degerfors",
            "134724",
            "Sirius",
            "2026-04-04",
            "13:00:00",
            null,
            "0",
            null,
            "FT",
            "no",
            "17256",
            "Stora Valla");

    assertThatThrownBy(() -> mapper.toMatch(event))
        .isInstanceOf(TheSportsDbMappingException.class)
        .hasMessageContaining("both score fields must be present or absent");
  }

  @Test
  void rejectsAnInvalidDate() {
    TheSportsDbEventDto event =
        new TheSportsDbEventDto(
            "2398752",
            "2026",
            "1",
            "134728",
            "Degerfors",
            "134724",
            "Sirius",
            "04/04/2026",
            "13:00:00",
            null,
            "0",
            "3",
            "FT",
            "no",
            "17256",
            "Stora Valla");

    assertThatThrownBy(() -> mapper.toMatch(event))
        .isInstanceOf(TheSportsDbMappingException.class)
        .hasMessageContaining("dateEvent must use ISO date format");
  }

  private TheSportsDbEventDto finishedEvent() {
    return new TheSportsDbEventDto(
        "2398752",
        "2026",
        "1",
        "134728",
        "Degerfors",
        "134724",
        "Sirius",
        "2026-04-04",
        "13:00:00",
        "2026-04-04T13:00:00",
        "0",
        "3",
        "FT",
        "no",
        "17256",
        "Stora Valla");
  }
}

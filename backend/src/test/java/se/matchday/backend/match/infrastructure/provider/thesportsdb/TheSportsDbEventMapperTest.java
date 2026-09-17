package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import se.matchday.backend.match.application.ProviderMatch;
import se.matchday.backend.match.domain.MatchStatus;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventDto;

class TheSportsDbEventMapperTest {

  private final TheSportsDbEventMapper mapper = new TheSportsDbEventMapper();

  @Test
  void mapsAFinishedMatchToTheProviderModel() {
    ProviderMatch match = mapper.toProviderMatch(finishedEvent());

    assertThat(match)
        .isEqualTo(
            new ProviderMatch(
                "2398752",
                2026,
                1,
                "134728",
                "Degerfors",
                "134724",
                "Sirius",
                LocalDate.of(2026, 4, 4),
                Instant.parse("2026-04-04T13:00:00Z"),
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

    ProviderMatch match = mapper.toProviderMatch(event);

    assertThat(match.kickoffAt()).isNull();
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

    assertThat(mapper.toProviderMatch(event).status()).isEqualTo(MatchStatus.POSTPONED);
  }

  @Test
  void normalizesAnOffsetTimestampToUtc() {
    TheSportsDbEventDto event =
        new TheSportsDbEventDto(
            "future-2",
            "2026",
            "2",
            "134011",
            "AIK",
            "134167",
            "Halmstad",
            "2026-04-12",
            "15:00:00",
            "2026-04-12T15:00:00+02:00",
            null,
            null,
            "NS",
            "no",
            null,
            null);

    assertThat(mapper.toProviderMatch(event).kickoffAt())
        .isEqualTo(Instant.parse("2026-04-12T13:00:00Z"));
  }

  @Test
  void mapsSoccerCompletionStatusesAsFinished() {
    for (String status : new String[] {"FT", "AET", "PEN"}) {
      TheSportsDbEventDto event =
          new TheSportsDbEventDto(
              "finished-" + status,
              "2026",
              "2",
              "134011",
              "AIK",
              "134167",
              "Halmstad",
              "2026-04-12",
              "13:00:00",
              "2026-04-12T13:00:00",
              "2",
              "1",
              status,
              "no",
              null,
              null);

      assertThat(mapper.toProviderMatch(event).status()).isEqualTo(MatchStatus.FINISHED);
    }
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

    assertThatThrownBy(() -> mapper.toProviderMatch(event))
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

    assertThatThrownBy(() -> mapper.toProviderMatch(event))
        .isInstanceOf(TheSportsDbMappingException.class)
        .hasMessage(
            "Invalid TheSportsDB event 2398752: both score fields must be present or absent")
        .hasCauseInstanceOf(IllegalArgumentException.class);
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

    assertThatThrownBy(() -> mapper.toProviderMatch(event))
        .isInstanceOf(TheSportsDbMappingException.class)
        .hasMessageContaining("dateEvent must use ISO date format");
  }

  @Test
  void rejectsAnInvalidTimestamp() {
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
            "not-a-timestamp",
            "0",
            "3",
            "FT",
            "no",
            "17256",
            "Stora Valla");

    assertThatThrownBy(() -> mapper.toProviderMatch(event))
        .isInstanceOf(TheSportsDbMappingException.class)
        .hasMessageContaining("strTimestamp must use ISO-8601 timestamp format");
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

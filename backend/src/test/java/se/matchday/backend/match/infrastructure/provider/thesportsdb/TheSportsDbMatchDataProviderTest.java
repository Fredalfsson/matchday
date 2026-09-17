package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import se.matchday.backend.match.domain.Match;
import se.matchday.backend.match.domain.MatchStatus;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventDto;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventsResponseDto;

class TheSportsDbMatchDataProviderTest {

  @Test
  void fetchesTheConfiguredLeagueAndMapsTheRound() {
    RecordingClient client =
        new RecordingClient(new TheSportsDbEventsResponseDto(List.of(event())));
    TheSportsDbMatchDataProvider provider = provider(client);

    List<Match> matches = provider.fetchRound(2026, 1);

    assertThat(client.apiKey).isEqualTo("test-key");
    assertThat(client.leagueId).isEqualTo("4347");
    assertThat(client.round).isEqualTo(1);
    assertThat(client.season).isEqualTo(2026);
    assertThat(matches)
        .containsExactly(
            new Match(
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
  void returnsAnEmptyListWhenTheProviderHasNoEvents() {
    TheSportsDbMatchDataProvider provider =
        provider(new RecordingClient(new TheSportsDbEventsResponseDto(null)));

    List<Match> matches = provider.fetchRound(2026, 1);

    assertThat(matches).isEmpty();
  }

  @Test
  void rejectsAnEmptyProviderResponseBody() {
    TheSportsDbMatchDataProvider provider = provider(new RecordingClient(null));

    assertThatThrownBy(() -> provider.fetchRound(2026, 1))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("TheSportsDB returned an empty response body");
  }

  @Test
  void rejectsInvalidRequestValuesBeforeCallingTheProvider() {
    RecordingClient client = new RecordingClient(new TheSportsDbEventsResponseDto(List.of()));
    TheSportsDbMatchDataProvider provider = provider(client);

    assertThatThrownBy(() -> provider.fetchRound(0, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("season must be a positive integer");
    assertThatThrownBy(() -> provider.fetchRound(2026, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("round must be a positive integer");
    assertThat(client.calls).isZero();
  }

  @Test
  void doesNotExposeTheApiKeyWhenConfigurationIsRendered() {
    TheSportsDbProperties properties = properties();

    assertThat(properties.toString()).doesNotContain("test-key").contains("apiKey=***");
  }

  private TheSportsDbMatchDataProvider provider(RecordingClient client) {
    TheSportsDbProperties properties = properties();
    TheSportsDbRequestExecutor requestExecutor =
        new TheSportsDbRequestExecutor(
            properties,
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
            ignored -> {});
    return new TheSportsDbMatchDataProvider(
        client, new TheSportsDbEventMapper(), properties, requestExecutor);
  }

  private TheSportsDbProperties properties() {
    return new TheSportsDbProperties(
        "test-key",
        "4347",
        Duration.ofMillis(2100),
        2,
        Duration.ofSeconds(60),
        Duration.ofSeconds(120));
  }

  private TheSportsDbEventDto event() {
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

  private static final class RecordingClient implements TheSportsDbClient {

    private final @Nullable TheSportsDbEventsResponseDto response;
    private String apiKey;
    private String leagueId;
    private int round;
    private int season;
    private int calls;

    private RecordingClient(@Nullable TheSportsDbEventsResponseDto response) {
      this.response = response;
    }

    @Override
    public @Nullable TheSportsDbEventsResponseDto getEventsByRound(
        String apiKey, String leagueId, int round, int season) {
      calls++;
      this.apiKey = apiKey;
      this.leagueId = leagueId;
      this.round = round;
      this.season = season;
      return response;
    }
  }
}

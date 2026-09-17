package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import java.util.List;
import org.jspecify.annotations.Nullable;
import se.matchday.backend.match.application.MatchDataProvider;
import se.matchday.backend.match.domain.Match;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventDto;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventsResponseDto;

final class TheSportsDbMatchDataProvider implements MatchDataProvider {

  private final TheSportsDbClient client;
  private final TheSportsDbEventMapper mapper;
  private final TheSportsDbProperties properties;

  TheSportsDbMatchDataProvider(
      TheSportsDbClient client, TheSportsDbEventMapper mapper, TheSportsDbProperties properties) {
    this.client = client;
    this.mapper = mapper;
    this.properties = properties;
  }

  @Override
  public List<Match> fetchRound(int season, int round) {
    requirePositive(season, "season");
    requirePositive(round, "round");

    @Nullable TheSportsDbEventsResponseDto response =
        client.getEventsByRound(properties.apiKey(), properties.leagueId(), round, season);
    if (response == null) {
      throw new IllegalStateException("TheSportsDB returned an empty response body");
    }

    List<TheSportsDbEventDto> events = response.events();
    if (events == null || events.isEmpty()) {
      return List.of();
    }
    return events.stream().map(mapper::toMatch).toList();
  }

  private void requirePositive(int value, String field) {
    if (value < 1) {
      throw new IllegalArgumentException(field + " must be a positive integer");
    }
  }
}

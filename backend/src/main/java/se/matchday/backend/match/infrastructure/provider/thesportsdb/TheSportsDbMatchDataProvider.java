package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.web.client.RestClientException;
import se.matchday.backend.match.application.InvalidMatchDataProviderResponseException;
import se.matchday.backend.match.application.MatchDataProvider;
import se.matchday.backend.match.application.MatchDataProviderUnavailableException;
import se.matchday.backend.match.application.ProviderMatch;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventDto;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventsResponseDto;

final class TheSportsDbMatchDataProvider implements MatchDataProvider {

  private final TheSportsDbClient client;
  private final TheSportsDbEventMapper mapper;
  private final TheSportsDbProperties properties;
  private final TheSportsDbRequestExecutor requestExecutor;

  TheSportsDbMatchDataProvider(
      TheSportsDbClient client,
      TheSportsDbEventMapper mapper,
      TheSportsDbProperties properties,
      TheSportsDbRequestExecutor requestExecutor) {
    this.client = client;
    this.mapper = mapper;
    this.properties = properties;
    this.requestExecutor = requestExecutor;
  }

  @Override
  public List<ProviderMatch> fetchRound(int season, int round) {
    requirePositive(season, "season");
    requirePositive(round, "round");

    @Nullable TheSportsDbEventsResponseDto response;
    try {
      response =
          requestExecutor.execute(
              () ->
                  client.getEventsByRound(
                      properties.apiKey(), properties.leagueId(), round, season));
    } catch (RestClientException exception) {
      throw new MatchDataProviderUnavailableException("TheSportsDB request failed", exception);
    }
    if (response == null) {
      throw new InvalidMatchDataProviderResponseException(
          "TheSportsDB returned an empty response body");
    }

    List<TheSportsDbEventDto> events = response.events();
    if (events == null || events.isEmpty()) {
      return List.of();
    }
    try {
      return events.stream().map(mapper::toProviderMatch).toList();
    } catch (TheSportsDbMappingException exception) {
      throw new InvalidMatchDataProviderResponseException(
          "TheSportsDB returned invalid match data", exception);
    }
  }

  private void requirePositive(int value, String field) {
    if (value < 1) {
      throw new IllegalArgumentException(field + " must be a positive integer");
    }
  }
}

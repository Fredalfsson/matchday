package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventsResponseDto;

@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface TheSportsDbClient {

  @GetExchange("/{apiKey}/eventsround.php")
  @Nullable TheSportsDbEventsResponseDto getEventsByRound(
      @PathVariable("apiKey") String apiKey,
      @RequestParam("id") String leagueId,
      @RequestParam("r") int round,
      @RequestParam("s") int season);
}

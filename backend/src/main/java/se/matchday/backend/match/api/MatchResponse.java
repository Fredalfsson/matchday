package se.matchday.backend.match.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import se.matchday.backend.match.domain.Match;
import se.matchday.backend.match.domain.MatchStatus;

record MatchResponse(
    UUID id,
    int season,
    int round,
    String homeTeamName,
    String awayTeamName,
    LocalDate scheduledDate,
    @Nullable Instant kickoffAt,
    MatchStatus status,
    @Nullable Integer homeScore,
    @Nullable Integer awayScore,
    @Nullable String venueName) {

  static MatchResponse from(Match match) {
    return new MatchResponse(
        match.id(),
        match.season(),
        match.round(),
        match.homeTeamName(),
        match.awayTeamName(),
        match.scheduledDate(),
        match.kickoffAt(),
        match.status(),
        match.homeScore(),
        match.awayScore(),
        match.venueName());
  }
}

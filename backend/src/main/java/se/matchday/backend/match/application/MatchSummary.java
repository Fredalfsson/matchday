package se.matchday.backend.match.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record MatchSummary(
    UUID id,
    int season,
    int round,
    String homeTeamName,
    String awayTeamName,
    LocalDate scheduledDate,
    @Nullable Instant kickoffAt,
    String status,
    @Nullable Integer homeScore,
    @Nullable Integer awayScore,
    @Nullable String venueName) {

  public MatchSummary {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(homeTeamName, "homeTeamName must not be null");
    Objects.requireNonNull(awayTeamName, "awayTeamName must not be null");
    Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");
    Objects.requireNonNull(status, "status must not be null");
  }
}

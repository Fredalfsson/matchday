package se.matchday.backend.match.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record Match(
    UUID id,
    int season,
    int round,
    String homeTeamName,
    String awayTeamName,
    LocalDate scheduledDate,
    @Nullable Instant kickoffAt,
    @Nullable Integer homeScore,
    @Nullable Integer awayScore,
    MatchStatus status,
    @Nullable String venueName) {

  public Match {
    Objects.requireNonNull(id, "id must not be null");
    requirePositive(season, "season");
    requirePositive(round, "round");
    requireText(homeTeamName, "homeTeamName");
    requireText(awayTeamName, "awayTeamName");
    Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");
    Objects.requireNonNull(status, "status must not be null");

    if ((homeScore == null) != (awayScore == null)) {
      throw new IllegalArgumentException("both score fields must be present or absent");
    }
    if ((homeScore != null && homeScore < 0) || (awayScore != null && awayScore < 0)) {
      throw new IllegalArgumentException("scores must be zero or greater");
    }
    if (status == MatchStatus.FINISHED && homeScore == null) {
      throw new IllegalArgumentException("a finished match must have a result");
    }
    if (status == MatchStatus.SCHEDULED && homeScore != null) {
      throw new IllegalArgumentException("a scheduled match cannot have a result");
    }
    if (venueName != null && venueName.isBlank()) {
      throw new IllegalArgumentException("venueName must not be blank");
    }
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }

  private static void requirePositive(int value, String field) {
    if (value < 1) {
      throw new IllegalArgumentException(field + " must be a positive integer");
    }
  }
}

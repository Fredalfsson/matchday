package se.matchday.backend.match.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import se.matchday.backend.match.domain.MatchStatus;

/** Provider-neutral match data accepted by the import use case. */
public record ProviderMatch(
    String externalMatchId,
    int season,
    int round,
    String homeTeamExternalId,
    String homeTeamName,
    String awayTeamExternalId,
    String awayTeamName,
    LocalDate scheduledDate,
    @Nullable Instant kickoffAt,
    @Nullable Integer homeScore,
    @Nullable Integer awayScore,
    MatchStatus status,
    @Nullable String venueName) {

  public ProviderMatch {
    requireText(externalMatchId, "externalMatchId");
    requirePositive(season, "season");
    requirePositive(round, "round");
    requireText(homeTeamExternalId, "homeTeamExternalId");
    requireText(homeTeamName, "homeTeamName");
    requireText(awayTeamExternalId, "awayTeamExternalId");
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

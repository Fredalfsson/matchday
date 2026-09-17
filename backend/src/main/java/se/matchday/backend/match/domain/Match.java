package se.matchday.backend.match.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import org.jspecify.annotations.Nullable;

public record Match(
    String externalId,
    int season,
    int round,
    String homeTeamId,
    String homeTeamName,
    String awayTeamId,
    String awayTeamName,
    LocalDate kickoffDate,
    @Nullable LocalTime kickoffTime,
    @Nullable Integer homeScore,
    @Nullable Integer awayScore,
    MatchStatus status,
    @Nullable String venueName) {}

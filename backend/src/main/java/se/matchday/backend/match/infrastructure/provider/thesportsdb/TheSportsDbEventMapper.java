package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import se.matchday.backend.match.domain.Match;
import se.matchday.backend.match.domain.MatchStatus;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventDto;

final class TheSportsDbEventMapper {

  Match toMatch(TheSportsDbEventDto event) {
    String externalId = required(event.eventId(), "idEvent");
    int season = positiveInteger(event.season(), "strSeason", externalId);
    int round = positiveInteger(event.round(), "intRound", externalId);
    String homeTeamId = required(event.homeTeamId(), "idHomeTeam", externalId);
    String homeTeamName = required(event.homeTeamName(), "strHomeTeam", externalId);
    String awayTeamId = required(event.awayTeamId(), "idAwayTeam", externalId);
    String awayTeamName = required(event.awayTeamName(), "strAwayTeam", externalId);
    LocalDate kickoffDate = date(event.eventDate(), "dateEvent", externalId);
    LocalTime kickoffTime = optionalTime(event.eventTime(), "strTime", externalId);
    Integer homeScore = optionalNonNegativeInteger(event.homeScore(), "intHomeScore", externalId);
    Integer awayScore = optionalNonNegativeInteger(event.awayScore(), "intAwayScore", externalId);

    requireCompleteScore(homeScore, awayScore, externalId);
    MatchStatus status = status(event.status(), event.postponed());
    requireConsistentResult(status, homeScore, externalId);

    return new Match(
        externalId,
        season,
        round,
        homeTeamId,
        homeTeamName,
        awayTeamId,
        awayTeamName,
        kickoffDate,
        kickoffTime,
        homeScore,
        awayScore,
        status,
        optionalText(event.venueName()));
  }

  private String required(@Nullable String value, String providerField) {
    if (value == null || value.isBlank()) {
      throw new TheSportsDbMappingException("Missing required field " + providerField);
    }
    return value.strip();
  }

  private String required(@Nullable String value, String providerField, String eventId) {
    if (value == null || value.isBlank()) {
      throw invalid(eventId, providerField, "is required");
    }
    return value.strip();
  }

  private int positiveInteger(@Nullable String value, String providerField, String eventId) {
    String requiredValue = required(value, providerField, eventId);
    try {
      int parsed = Integer.parseInt(requiredValue);
      if (parsed < 1) {
        throw invalid(eventId, providerField, "must be a positive integer");
      }
      return parsed;
    } catch (NumberFormatException exception) {
      throw invalid(eventId, providerField, "must be a positive integer", exception);
    }
  }

  private @Nullable Integer optionalNonNegativeInteger(
      @Nullable String value, String providerField, String eventId) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      int parsed = Integer.parseInt(value.strip());
      if (parsed < 0) {
        throw invalid(eventId, providerField, "must be zero or greater");
      }
      return parsed;
    } catch (NumberFormatException exception) {
      throw invalid(eventId, providerField, "must be zero or greater", exception);
    }
  }

  private LocalDate date(@Nullable String value, String providerField, String eventId) {
    String requiredValue = required(value, providerField, eventId);
    try {
      return LocalDate.parse(requiredValue);
    } catch (DateTimeException exception) {
      throw invalid(eventId, providerField, "must use ISO date format", exception);
    }
  }

  private @Nullable LocalTime optionalTime(
      @Nullable String value, String providerField, String eventId) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalTime.parse(value.strip());
    } catch (DateTimeException exception) {
      throw invalid(eventId, providerField, "must use ISO time format", exception);
    }
  }

  private MatchStatus status(@Nullable String providerStatus, @Nullable String postponed) {
    if ("yes".equalsIgnoreCase(postponed) || "PST".equalsIgnoreCase(providerStatus)) {
      return MatchStatus.POSTPONED;
    }
    if (providerStatus == null || providerStatus.isBlank()) {
      return MatchStatus.UNKNOWN;
    }
    return switch (providerStatus.strip().toUpperCase(Locale.ROOT)) {
      case "NS", "TBD" -> MatchStatus.SCHEDULED;
      case "FT" -> MatchStatus.FINISHED;
      default -> MatchStatus.UNKNOWN;
    };
  }

  private void requireCompleteScore(
      @Nullable Integer homeScore, @Nullable Integer awayScore, String eventId) {
    if ((homeScore == null) != (awayScore == null)) {
      throw new TheSportsDbMappingException(
          "Invalid TheSportsDB event " + eventId + ": both score fields must be present or absent");
    }
  }

  private void requireConsistentResult(
      MatchStatus status, @Nullable Integer homeScore, String eventId) {
    if (status == MatchStatus.FINISHED && homeScore == null) {
      throw new TheSportsDbMappingException(
          "Invalid TheSportsDB event " + eventId + ": a finished match must have a result");
    }
    if (status == MatchStatus.SCHEDULED && homeScore != null) {
      throw new TheSportsDbMappingException(
          "Invalid TheSportsDB event " + eventId + ": a scheduled match cannot have a result");
    }
  }

  private @Nullable String optionalText(@Nullable String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private TheSportsDbMappingException invalid(
      String eventId, String providerField, String requirement) {
    return new TheSportsDbMappingException(
        "Invalid TheSportsDB event " + eventId + ": " + providerField + " " + requirement);
  }

  private TheSportsDbMappingException invalid(
      String eventId, String providerField, String requirement, Throwable cause) {
    return new TheSportsDbMappingException(
        "Invalid TheSportsDB event " + eventId + ": " + providerField + " " + requirement, cause);
  }
}

package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import se.matchday.backend.match.application.ProviderMatch;
import se.matchday.backend.match.domain.MatchStatus;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventDto;

final class TheSportsDbEventMapper {

  ProviderMatch toProviderMatch(TheSportsDbEventDto event) {
    String externalMatchId = required(event.eventId(), "idEvent");
    int season = positiveInteger(event.season(), "strSeason", externalMatchId);
    int round = positiveInteger(event.round(), "intRound", externalMatchId);
    String homeTeamExternalId = required(event.homeTeamId(), "idHomeTeam", externalMatchId);
    String homeTeamName = required(event.homeTeamName(), "strHomeTeam", externalMatchId);
    String awayTeamExternalId = required(event.awayTeamId(), "idAwayTeam", externalMatchId);
    String awayTeamName = required(event.awayTeamName(), "strAwayTeam", externalMatchId);
    LocalDate scheduledDate = date(event.eventDate(), "dateEvent", externalMatchId);
    Instant kickoffAt = optionalUtcTimestamp(event.timestamp(), "strTimestamp", externalMatchId);
    Integer homeScore =
        optionalNonNegativeInteger(event.homeScore(), "intHomeScore", externalMatchId);
    Integer awayScore =
        optionalNonNegativeInteger(event.awayScore(), "intAwayScore", externalMatchId);

    MatchStatus status = status(event.status(), event.postponed());

    try {
      return new ProviderMatch(
          externalMatchId,
          season,
          round,
          homeTeamExternalId,
          homeTeamName,
          awayTeamExternalId,
          awayTeamName,
          scheduledDate,
          kickoffAt,
          homeScore,
          awayScore,
          status,
          optionalText(event.venueName()));
    } catch (IllegalArgumentException exception) {
      throw new TheSportsDbMappingException(
          "Invalid TheSportsDB event " + externalMatchId + ": " + exception.getMessage(),
          exception);
    }
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

  private @Nullable Instant optionalUtcTimestamp(
      @Nullable String value, String providerField, String eventId) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String timestamp = value.strip();
    try {
      return OffsetDateTime.parse(timestamp).toInstant();
    } catch (DateTimeException offsetException) {
      try {
        return LocalDateTime.parse(timestamp).toInstant(ZoneOffset.UTC);
      } catch (DateTimeException localException) {
        throw invalid(eventId, providerField, "must use ISO-8601 timestamp format", localException);
      }
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
      case "FT", "AET", "PEN" -> MatchStatus.FINISHED;
      default -> MatchStatus.UNKNOWN;
    };
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

package se.matchday.backend.match.infrastructure.provider.thesportsdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TheSportsDbEventDto(
    @JsonProperty("idEvent") @Nullable String eventId,
    @JsonProperty("strSeason") @Nullable String season,
    @JsonProperty("intRound") @Nullable String round,
    @JsonProperty("idHomeTeam") @Nullable String homeTeamId,
    @JsonProperty("strHomeTeam") @Nullable String homeTeamName,
    @JsonProperty("idAwayTeam") @Nullable String awayTeamId,
    @JsonProperty("strAwayTeam") @Nullable String awayTeamName,
    @JsonProperty("dateEvent") @Nullable String eventDate,
    @JsonProperty("strTime") @Nullable String eventTime,
    @JsonProperty("strTimestamp") @Nullable String timestamp,
    @JsonProperty("intHomeScore") @Nullable String homeScore,
    @JsonProperty("intAwayScore") @Nullable String awayScore,
    @JsonProperty("strStatus") @Nullable String status,
    @JsonProperty("strPostponed") @Nullable String postponed,
    @JsonProperty("idVenue") @Nullable String venueId,
    @JsonProperty("strVenue") @Nullable String venueName) {}

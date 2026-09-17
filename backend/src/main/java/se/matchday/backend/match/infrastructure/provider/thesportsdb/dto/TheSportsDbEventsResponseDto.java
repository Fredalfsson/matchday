package se.matchday.backend.match.infrastructure.provider.thesportsdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TheSportsDbEventsResponseDto(
    @JsonProperty("events") @Nullable List<TheSportsDbEventDto> events) {}

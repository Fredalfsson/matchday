package se.matchday.backend.match.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

record MatchImportRequest(@NotNull @Positive Integer season) {}

package se.matchday.backend.match.api;

import jakarta.validation.constraints.Positive;

record MatchImportRequest(@Positive int season) {}

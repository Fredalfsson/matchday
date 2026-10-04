package se.matchday.backend.message.api;

import jakarta.validation.constraints.NotBlank;

record MessageCreationRequest(@NotBlank String content) {}

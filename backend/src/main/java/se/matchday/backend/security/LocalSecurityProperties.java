package se.matchday.backend.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("matchday.security.local")
record LocalSecurityProperties(@NotBlank String username, @NotBlank String password) {}

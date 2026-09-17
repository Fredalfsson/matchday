package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param apiKey API key used in TheSportsDB v1 request paths
 * @param leagueId TheSportsDB league identifier for Allsvenskan
 * @param minimumRequestInterval minimum time between request starts
 * @param maxRetries maximum retry attempts after an HTTP 429 response
 * @param fallbackRetryDelay delay used when Retry-After is missing or invalid
 * @param maximumRetryDelay largest Retry-After delay the importer will wait for
 */
@Validated
@ConfigurationProperties("matchday.providers.the-sports-db")
record TheSportsDbProperties(
    @NotBlank String apiKey,
    @NotBlank String leagueId,
    @NotNull Duration minimumRequestInterval,
    @Min(0) @Max(3) int maxRetries,
    @NotNull Duration fallbackRetryDelay,
    @NotNull Duration maximumRetryDelay) {

  private static final Duration FREE_TIER_MINIMUM_INTERVAL = Duration.ofMillis(2100);

  TheSportsDbProperties {
    Objects.requireNonNull(minimumRequestInterval, "minimumRequestInterval must not be null");
    Objects.requireNonNull(fallbackRetryDelay, "fallbackRetryDelay must not be null");
    Objects.requireNonNull(maximumRetryDelay, "maximumRetryDelay must not be null");
    if (minimumRequestInterval.compareTo(FREE_TIER_MINIMUM_INTERVAL) < 0) {
      throw new IllegalArgumentException("minimumRequestInterval must be at least 2100ms");
    }
    if (maxRetries < 0 || maxRetries > 3) {
      throw new IllegalArgumentException("maxRetries must be between 0 and 3");
    }
    if (fallbackRetryDelay.isNegative() || fallbackRetryDelay.isZero()) {
      throw new IllegalArgumentException("fallbackRetryDelay must be greater than zero");
    }
    if (maximumRetryDelay.isNegative() || maximumRetryDelay.isZero()) {
      throw new IllegalArgumentException("maximumRetryDelay must be greater than zero");
    }
    if (fallbackRetryDelay.compareTo(maximumRetryDelay) > 0) {
      throw new IllegalArgumentException("fallbackRetryDelay must not exceed maximumRetryDelay");
    }
  }

  @Override
  public String toString() {
    return "TheSportsDbProperties[apiKey=***, leagueId="
        + leagueId
        + ", minimumRequestInterval="
        + minimumRequestInterval
        + ", maxRetries="
        + maxRetries
        + ", fallbackRetryDelay="
        + fallbackRetryDelay
        + ", maximumRetryDelay="
        + maximumRetryDelay
        + "]";
  }
}

package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TheSportsDbPropertiesTest {

  @Test
  void rejectsARequestIntervalWithoutAFreeTierSafetyMargin() {
    assertThatThrownBy(
            () ->
                properties(
                    Duration.ofSeconds(2), 2, Duration.ofSeconds(60), Duration.ofSeconds(120)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("minimumRequestInterval must be at least 2100ms");
  }

  @Test
  void rejectsAnUnboundedNumberOfRetries() {
    assertThatThrownBy(
            () ->
                properties(
                    Duration.ofMillis(2100), 4, Duration.ofSeconds(60), Duration.ofSeconds(120)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("maxRetries must be between 0 and 3");
  }

  @Test
  void rejectsAFallbackBeyondTheMaximumRetryDelay() {
    assertThatThrownBy(
            () ->
                properties(
                    Duration.ofMillis(2100), 2, Duration.ofSeconds(121), Duration.ofSeconds(120)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("fallbackRetryDelay must not exceed maximumRetryDelay");
  }

  private TheSportsDbProperties properties(
      Duration minimumRequestInterval,
      int maxRetries,
      Duration fallbackRetryDelay,
      Duration maximumRetryDelay) {
    return new TheSportsDbProperties(
        "test-key",
        "4347",
        minimumRequestInterval,
        maxRetries,
        fallbackRetryDelay,
        maximumRetryDelay);
  }
}

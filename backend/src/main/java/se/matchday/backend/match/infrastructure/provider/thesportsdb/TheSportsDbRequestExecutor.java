package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;

final class TheSportsDbRequestExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(TheSportsDbRequestExecutor.class);

  private final Duration minimumRequestInterval;
  private final int maxRetries;
  private final Duration fallbackRetryDelay;
  private final Duration maximumRetryDelay;
  private final Clock clock;
  private final Sleeper sleeper;

  private @Nullable Instant lastRequestStartedAt;

  TheSportsDbRequestExecutor(TheSportsDbProperties properties, Clock clock, Sleeper sleeper) {
    this.minimumRequestInterval = properties.minimumRequestInterval();
    this.maxRetries = properties.maxRetries();
    this.fallbackRetryDelay = properties.fallbackRetryDelay();
    this.maximumRetryDelay = properties.maximumRetryDelay();
    this.clock = clock;
    this.sleeper = sleeper;
  }

  synchronized <T extends @Nullable Object> T execute(Supplier<T> request) {
    int retries = 0;
    while (true) {
      awaitRequestSlot();
      try {
        return request.get();
      } catch (HttpClientErrorException.TooManyRequests exception) {
        if (retries >= maxRetries) {
          throw exception;
        }

        Duration retryDelay = retryDelay(exception);
        if (retryDelay.compareTo(maximumRetryDelay) > 0) {
          throw exception;
        }

        retries++;
        LOGGER.warn(
            "TheSportsDB rate limit reached; retrying attempt {} of {} after {}",
            retries,
            maxRetries,
            retryDelay);
        sleep(retryDelay);
      }
    }
  }

  private void awaitRequestSlot() {
    Instant now = clock.instant();
    if (lastRequestStartedAt != null) {
      Instant earliestNextRequest = lastRequestStartedAt.plus(minimumRequestInterval);
      Duration wait = Duration.between(now, earliestNextRequest);
      if (wait.isPositive()) {
        sleep(wait);
      }
    }
    lastRequestStartedAt = clock.instant();
  }

  private Duration retryDelay(HttpClientErrorException.TooManyRequests exception) {
    @Nullable HttpHeaders responseHeaders = exception.getResponseHeaders();
    @Nullable String retryAfter =
        responseHeaders != null ? responseHeaders.getFirst(HttpHeaders.RETRY_AFTER) : null;
    @Nullable Duration parsed = parseRetryAfter(retryAfter);
    return parsed != null ? parsed : fallbackRetryDelay;
  }

  private @Nullable Duration parseRetryAfter(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    String retryAfter = value.strip();
    try {
      long seconds = Long.parseLong(retryAfter);
      return seconds < 0 ? null : Duration.ofSeconds(seconds);
    } catch (NumberFormatException | ArithmeticException ignored) {
      // Retry-After also permits an HTTP date.
    }

    try {
      Instant retryAt =
          ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
      Duration delay = Duration.between(clock.instant(), retryAt);
      return delay.isNegative() ? Duration.ZERO : delay;
    } catch (DateTimeException ignored) {
      return null;
    }
  }

  private void sleep(Duration duration) {
    if (duration.isZero()) {
      return;
    }
    try {
      sleeper.sleep(duration);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting to call TheSportsDB", exception);
    }
  }

  @FunctionalInterface
  interface Sleeper {
    void sleep(Duration duration) throws InterruptedException;
  }
}

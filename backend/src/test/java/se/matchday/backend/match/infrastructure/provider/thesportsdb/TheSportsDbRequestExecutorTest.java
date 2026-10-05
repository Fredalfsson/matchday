package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class TheSportsDbRequestExecutorTest {

  private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

  @Test
  void spacesRequestStartsBelowTheFreeTierLimit() {
    TestTime time = new TestTime(START);
    TheSportsDbRequestExecutor executor = executor(time, 2);

    executor.execute(() -> "first");
    executor.execute(() -> "second");

    assertThat(time.sleeps()).containsExactly(Duration.ofMillis(2100));
  }

  @Test
  void retriesAfterTheDelaySpecifiedInSeconds() {
    TestTime time = new TestTime(START);
    TheSportsDbRequestExecutor executor = executor(time, 2);
    AtomicInteger attempts = new AtomicInteger();

    String result =
        executor.execute(
            () -> {
              if (attempts.getAndIncrement() == 0) {
                throw tooManyRequests("3");
              }
              return "success";
            });

    assertThat(result).isEqualTo("success");
    assertThat(attempts).hasValue(2);
    assertThat(time.sleeps()).containsExactly(Duration.ofSeconds(3));
  }

  @Test
  void supportsAnHttpDateInRetryAfter() {
    TestTime time = new TestTime(START);
    TheSportsDbRequestExecutor executor = executor(time, 2);
    AtomicInteger attempts = new AtomicInteger();
    String retryAt =
        DateTimeFormatter.RFC_1123_DATE_TIME.format(
            ZonedDateTime.ofInstant(START.plusSeconds(10), ZoneOffset.UTC));

    executor.execute(
        () -> {
          if (attempts.getAndIncrement() == 0) {
            throw tooManyRequests(retryAt);
          }
          return "success";
        });

    assertThat(time.sleeps()).containsExactly(Duration.ofSeconds(10));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "invalid"})
  void usesTheFallbackDelayWhenRetryAfterIsUnavailable(@Nullable String retryAfter) {
    TestTime time = new TestTime(START);
    TheSportsDbRequestExecutor executor = executor(time, 2);
    AtomicInteger attempts = new AtomicInteger();

    executor.execute(
        () -> {
          if (attempts.getAndIncrement() == 0) {
            throw tooManyRequests(retryAfter);
          }
          return "success";
        });

    assertThat(time.sleeps()).containsExactly(Duration.ofSeconds(60));
  }

  @Test
  void stopsAfterTheConfiguredNumberOfRetries() {
    TestTime time = new TestTime(START);
    TheSportsDbRequestExecutor executor = executor(time, 2);
    AtomicInteger attempts = new AtomicInteger();

    assertThatThrownBy(
            () ->
                executor.execute(
                    () -> {
                      attempts.incrementAndGet();
                      throw tooManyRequests("3");
                    }))
        .isInstanceOf(HttpClientErrorException.TooManyRequests.class);

    assertThat(attempts).hasValue(3);
    assertThat(time.sleeps()).containsExactly(Duration.ofSeconds(3), Duration.ofSeconds(3));
  }

  @Test
  void doesNotRetryEarlierThanAnExcessiveRetryAfter() {
    TestTime time = new TestTime(START);
    TheSportsDbRequestExecutor executor = executor(time, 2);
    AtomicInteger attempts = new AtomicInteger();

    assertThatThrownBy(
            () ->
                executor.execute(
                    () -> {
                      attempts.incrementAndGet();
                      throw tooManyRequests("121");
                    }))
        .isInstanceOf(HttpClientErrorException.TooManyRequests.class);

    assertThat(attempts).hasValue(1);
    assertThat(time.sleeps()).isEmpty();
  }

  @Test
  void preservesTheInterruptedStatusWhenWaitingIsInterrupted() {
    TheSportsDbProperties properties =
        new TheSportsDbProperties(
            "test-key",
            "4347",
            Duration.ofMillis(2100),
            2,
            Duration.ofSeconds(60),
            Duration.ofSeconds(120));
    TheSportsDbRequestExecutor executor =
        new TheSportsDbRequestExecutor(
            properties,
            Clock.fixed(START, ZoneOffset.UTC),
            ignored -> {
              throw new InterruptedException("test interruption");
            });

    executor.execute(() -> "first");
    try {
      assertThatThrownBy(() -> executor.execute(() -> "second"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Interrupted while waiting to call TheSportsDB")
          .hasCauseInstanceOf(InterruptedException.class);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  private TheSportsDbRequestExecutor executor(TestTime time, int maxRetries) {
    TheSportsDbProperties properties =
        new TheSportsDbProperties(
            "test-key",
            "4347",
            Duration.ofMillis(2100),
            maxRetries,
            Duration.ofSeconds(60),
            Duration.ofSeconds(120));
    return new TheSportsDbRequestExecutor(properties, time, time::sleep);
  }

  private HttpClientErrorException.TooManyRequests tooManyRequests(@Nullable String retryAfter) {
    @Nullable HttpHeaders headers = null;
    if (retryAfter != null) {
      headers = new HttpHeaders();
      headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
    }
    return (HttpClientErrorException.TooManyRequests)
        HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, new byte[0], UTF_8);
  }

  private static final class TestTime extends Clock {

    private Instant current;
    private final List<Duration> sleeps = new ArrayList<>();

    private TestTime(Instant current) {
      this.current = current;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      if (!ZoneOffset.UTC.equals(zone)) {
        throw new IllegalArgumentException("Only UTC is supported by this test clock");
      }
      return this;
    }

    @Override
    public Instant instant() {
      return current;
    }

    private void sleep(Duration duration) {
      sleeps.add(duration);
      current = current.plus(duration);
    }

    private List<Duration> sleeps() {
      return List.copyOf(sleeps);
    }
  }
}

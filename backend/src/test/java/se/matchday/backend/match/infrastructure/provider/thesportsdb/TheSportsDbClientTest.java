package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventDto;
import se.matchday.backend.match.infrastructure.provider.thesportsdb.dto.TheSportsDbEventsResponseDto;

class TheSportsDbClientTest {

  private TheSportsDbClient client;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder =
        RestClient.builder().baseUrl("https://provider.example/api/v1/json");
    server = MockRestServiceServer.bindTo(builder).build();
    client =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
            .build()
            .createClient(TheSportsDbClient.class);
  }

  @Test
  void requestsOneRoundAndDeserializesTheProviderResponse() throws IOException {
    server
        .expect(
            requestTo(
                "https://provider.example/api/v1/json/test-key/eventsround.php?id=4347&r=1&s=2026"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess(fixture(), MediaType.APPLICATION_JSON));

    TheSportsDbEventsResponseDto response =
        Objects.requireNonNull(
            client.getEventsByRound("test-key", "4347", 1, 2026),
            "The fixture should produce a response body");

    List<TheSportsDbEventDto> events =
        Objects.requireNonNull(response.events(), "The fixture should contain events");
    assertThat(events)
        .containsExactly(
            new TheSportsDbEventDto(
                "2398752",
                "2026",
                "1",
                "134728",
                "Degerfors",
                "134724",
                "Sirius",
                "2026-04-04",
                "13:00:00",
                "2026-04-04T13:00:00",
                "0",
                "3",
                "FT",
                "no",
                "17256",
                "Stora Valla"),
            new TheSportsDbEventDto(
                "future-1",
                "2026",
                "1",
                "134011",
                "AIK",
                "134167",
                "Halmstad",
                "2026-04-05",
                null,
                null,
                null,
                null,
                "NS",
                "no",
                null,
                null));

    server.verify();
  }

  @Test
  void deserializesNullEventsWhenTheProviderHasNoMatches() {
    server
        .expect(
            requestTo(
                "https://provider.example/api/v1/json/test-key/eventsround.php?id=4347&r=31&s=2026"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
        .andRespond(withSuccess("{\"events\":null}", MediaType.APPLICATION_JSON));

    TheSportsDbEventsResponseDto response =
        Objects.requireNonNull(
            client.getEventsByRound("test-key", "4347", 31, 2026),
            "The fixture should produce a response body");

    assertThat(response.events()).isNull();

    server.verify();
  }

  @Test
  void exposesTooManyRequestsAndItsRetryAfterHeader() {
    server
        .expect(
            requestTo(
                "https://provider.example/api/v1/json/test-key/eventsround.php?id=4347&r=1&s=2026"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "60"));

    assertThatThrownBy(() -> client.getEventsByRound("test-key", "4347", 1, 2026))
        .isInstanceOf(HttpClientErrorException.TooManyRequests.class)
        .satisfies(
            exception ->
                assertThat(
                        ((HttpClientErrorException.TooManyRequests) exception)
                            .getResponseHeaders()
                            .getFirst(HttpHeaders.RETRY_AFTER))
                    .isEqualTo("60"));

    server.verify();
  }

  private String fixture() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/fixtures/thesportsdb/round-1.json")) {
      if (input == null) {
        throw new IOException("TheSportsDB test fixture is missing");
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}

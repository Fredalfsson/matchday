package se.matchday.backend.match.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import se.matchday.backend.TestcontainersConfiguration;
import se.matchday.backend.match.application.MatchRepository;
import se.matchday.backend.match.application.ProviderMatch;
import se.matchday.backend.match.domain.Match;
import se.matchday.backend.match.domain.MatchStatus;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class MatchControllerIntegrationTest {

  private final MockMvc mockMvc;
  private final MatchRepository repository;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  MatchControllerIntegrationTest(
      MockMvc mockMvc, MatchRepository repository, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.repository = repository;
    this.jdbcTemplate = jdbcTemplate;
  }

  @BeforeEach
  void clearMatches() {
    jdbcTemplate.update("TRUNCATE TABLE matches");
  }

  @Test
  void returnsAnEmptyArrayWhenNoMatchesAreStored() throws Exception {
    mockMvc
        .perform(get("/api/v1/matches"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(content().json("[]"));
  }

  @Test
  void returnsStoredMatchesWithoutAuthenticationOrProviderIdentifiers() throws Exception {
    ProviderMatch providerMatch =
        new ProviderMatch(
            "provider-event-1",
            2026,
            1,
            "provider-home-1",
            "Degerfors",
            "provider-away-1",
            "Sirius",
            LocalDate.of(2026, 4, 4),
            Instant.parse("2026-04-04T13:00:00Z"),
            0,
            3,
            MatchStatus.FINISHED,
            "Stora Valla");
    repository.saveAll(List.of(providerMatch));
    Match match = repository.findAll().getFirst();

    mockMvc
        .perform(get("/api/v1/matches"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$[0].id").value(match.id().toString()))
        .andExpect(jsonPath("$[0].season").value(2026))
        .andExpect(jsonPath("$[0].round").value(1))
        .andExpect(jsonPath("$[0].homeTeamName").value("Degerfors"))
        .andExpect(jsonPath("$[0].awayTeamName").value("Sirius"))
        .andExpect(jsonPath("$[0].scheduledDate").value("2026-04-04"))
        .andExpect(jsonPath("$[0].kickoffAt").value("2026-04-04T13:00:00Z"))
        .andExpect(jsonPath("$[0].status").value("FINISHED"))
        .andExpect(jsonPath("$[0].homeScore").value(0))
        .andExpect(jsonPath("$[0].awayScore").value(3))
        .andExpect(jsonPath("$[0].venueName").value("Stora Valla"))
        .andExpect(jsonPath("$[0].externalMatchId").doesNotExist())
        .andExpect(jsonPath("$[0].homeTeamExternalId").doesNotExist())
        .andExpect(jsonPath("$[0].awayTeamExternalId").doesNotExist());
  }
}

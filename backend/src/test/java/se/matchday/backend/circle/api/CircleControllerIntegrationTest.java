package se.matchday.backend.circle.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.matchday.backend.TestcontainersConfiguration;
import se.matchday.backend.identity.application.CurrentUser;
import se.matchday.backend.match.application.MatchRepository;
import se.matchday.backend.match.application.ProviderMatch;
import se.matchday.backend.match.domain.MatchStatus;

@Import({
  TestcontainersConfiguration.class,
  CircleControllerIntegrationTest.TestIdentityConfiguration.class
})
@AutoConfigureMockMvc
@SpringBootTest
class CircleControllerIntegrationTest {

  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

  private final MockMvc mockMvc;
  private final MatchRepository matchRepository;
  private final JdbcTemplate jdbcTemplate;
  private final TestCurrentUser currentUser;

  @Autowired
  CircleControllerIntegrationTest(
      MockMvc mockMvc,
      MatchRepository matchRepository,
      JdbcTemplate jdbcTemplate,
      TestCurrentUser currentUser) {
    this.mockMvc = mockMvc;
    this.matchRepository = matchRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.currentUser = currentUser;
  }

  @BeforeEach
  void prepareTest() {
    clearDatabase();
    currentUser.authenticateAs(USER_ID);
  }

  @AfterEach
  void cleanUpTest() {
    clearDatabase();
  }

  private void clearDatabase() {
    jdbcTemplate.update("TRUNCATE TABLE circle_memberships, circles, matches");
  }

  @Test
  void rejectsAnAnonymousUser() throws Exception {
    UUID matchId = storeMatch();

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/circle", matchId).with(csrf()))
        .andExpect(status().isUnauthorized());

    assertNoCircleWasStored();
  }

  @Test
  @WithMockUser
  void rejectsCreationWhenTheAuthenticatedIdentityCannotBeResolved() throws Exception {
    UUID matchId = storeMatch();
    currentUser.clear();

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/circle", matchId).with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:authentication-required"))
        .andExpect(jsonPath("$.title").value("Authentication required"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("An authenticated user identity is required"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoCircleWasStored();
  }

  @Test
  @WithMockUser
  void createsCircleAndCreatorMembershipAtomically() throws Exception {
    UUID matchId = storeMatch();

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/circle", matchId).with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/matches/" + matchId + "/circle"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.matchId").value(matchId.toString()))
        .andExpect(jsonPath("$.createdAt").isNotEmpty())
        .andExpect(jsonPath("$.membershipActive").value(true))
        .andExpect(jsonPath("$.createdByUserId").doesNotExist());

    assertThat(rowCount("circles")).isOne();
    assertThat(rowCount("circle_memberships")).isOne();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM circle_memberships WHERE user_id = ?",
                Integer.class,
                USER_ID))
        .isOne();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM circle_memberships membership
                JOIN circles circle ON circle.id = membership.circle_id
                WHERE circle.match_id = ?
                  AND circle.created_by_user_id = membership.user_id
                  AND circle.created_at = membership.joined_at
                """,
                Integer.class,
                matchId))
        .isOne();
  }

  @Test
  @WithMockUser
  void rejectsAnUnknownMatch() throws Exception {
    UUID unknownMatchId = UUID.fromString("10000000-0000-0000-0000-000000000099");

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/circle", unknownMatchId).with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:match-not-found"))
        .andExpect(jsonPath("$.title").value("Match not found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Match " + unknownMatchId + " was not found"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoCircleWasStored();
  }

  @Test
  @WithMockUser
  void rejectsASecondCircleForTheSameMatch() throws Exception {
    UUID matchId = storeMatch();
    mockMvc
        .perform(post("/api/v1/matches/{matchId}/circle", matchId).with(csrf()))
        .andExpect(status().isCreated());

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/circle", matchId).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:circle-already-exists"))
        .andExpect(jsonPath("$.title").value("Circle already exists"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.detail").value("A circle already exists for match " + matchId))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertThat(rowCount("circles")).isOne();
    assertThat(rowCount("circle_memberships")).isOne();
  }

  private UUID storeMatch() {
    matchRepository.saveAll(
        List.of(
            new ProviderMatch(
                "circle-controller-test-match",
                2026,
                1,
                "home-1",
                "Home",
                "away-1",
                "Away",
                LocalDate.of(2026, 4, 4),
                Instant.parse("2026-04-04T13:00:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED,
                null)));
    return matchRepository.findAll().getFirst().id();
  }

  private void assertNoCircleWasStored() {
    assertThat(rowCount("circles")).isZero();
    assertThat(rowCount("circle_memberships")).isZero();
  }

  private int rowCount(String table) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestIdentityConfiguration {

    @Bean
    @Primary
    TestCurrentUser testCurrentUser() {
      return new TestCurrentUser();
    }
  }

  static final class TestCurrentUser implements CurrentUser {

    private Optional<UUID> userId = Optional.empty();

    @Override
    public Optional<UUID> userId() {
      return userId;
    }

    void authenticateAs(UUID userId) {
      this.userId = Optional.of(userId);
    }

    void clear() {
      userId = Optional.empty();
    }
  }
}

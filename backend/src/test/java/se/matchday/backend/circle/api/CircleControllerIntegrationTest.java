package se.matchday.backend.circle.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

  private static final String USERNAME = "circle-user";
  private static final String SECOND_USERNAME = "second-circle-user";
  private static final String UNMAPPED_USERNAME = "unmapped-circle-user";
  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID SECOND_USER_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000002");

  private final MockMvc mockMvc;
  private final MatchRepository matchRepository;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  CircleControllerIntegrationTest(
      MockMvc mockMvc, MatchRepository matchRepository, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.matchRepository = matchRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  @BeforeEach
  void prepareTest() {
    clearDatabase();
  }

  @AfterEach
  void cleanUpTest() {
    clearDatabase();
  }

  private void clearDatabase() {
    jdbcTemplate.update("TRUNCATE TABLE messages, circle_memberships, circles, matches");
  }

  @Test
  void rejectsAnonymousCircleCreation() throws Exception {
    UUID matchId = storeMatch();

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/circle", matchId).with(csrf()))
        .andExpect(status().isUnauthorized());

    assertNoCircleWasStored();
  }

  @Test
  @WithMockUser(username = UNMAPPED_USERNAME)
  void rejectsCreationWhenTheAuthenticatedIdentityCannotBeResolved() throws Exception {
    UUID matchId = storeMatch();

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
  @WithMockUser(username = USERNAME)
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
        .andExpect(jsonPath("$.createdByUserId").doesNotExist())
        .andExpect(jsonPath("$.email").doesNotExist());

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
  @WithMockUser(username = USERNAME)
  void rejectsCircleCreationForAnUnknownMatch() throws Exception {
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
  @WithMockUser(username = USERNAME)
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

  @Test
  @WithMockUser(username = USERNAME)
  void rejectsCreationWithoutCsrfProtection() throws Exception {
    UUID matchId = storeMatch();

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/circle", matchId))
        .andExpect(status().isForbidden());

    assertNoCircleWasStored();
  }

  @Test
  @WithMockUser(username = USERNAME)
  void rejectsCircleCreationWithAMalformedMatchId() throws Exception {
    storeMatch();

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/circle", "not-a-uuid").with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-request"))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("The request path contains an invalid value"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoCircleWasStored();
  }

  @Test
  void allowsOnlyOneOfTwoConcurrentUsersToCreateTheCircle() throws Exception {
    UUID matchId = storeMatch();
    CountDownLatch requestsReady = new CountDownLatch(2);
    CountDownLatch startRequests = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<CreationAttempt> firstAttempt =
          executor.submit(
              () ->
                  createCircleWhenReleased(
                      matchId, USERNAME, USER_ID, requestsReady, startRequests));
      Future<CreationAttempt> secondAttempt =
          executor.submit(
              () ->
                  createCircleWhenReleased(
                      matchId, SECOND_USERNAME, SECOND_USER_ID, requestsReady, startRequests));

      assertThat(requestsReady.await(5, TimeUnit.SECONDS)).isTrue();
      startRequests.countDown();

      List<CreationAttempt> attempts =
          List.of(firstAttempt.get(10, TimeUnit.SECONDS), secondAttempt.get(10, TimeUnit.SECONDS));

      assertThat(attempts)
          .extracting(attempt -> attempt.status())
          .containsExactlyInAnyOrder(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());

      CreationAttempt winner =
          attempts.stream()
              .filter(attempt -> attempt.status() == HttpStatus.CREATED.value())
              .findFirst()
              .orElseThrow();

      assertThat(rowCount("circles")).isOne();
      assertThat(rowCount("circle_memberships")).isOne();
      assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM circle_memberships", UUID.class))
          .isEqualTo(winner.userId());
    } finally {
      startRequests.countDown();
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void joinsAnExistingCircleAndReportsActiveMembership() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);

    mockMvc
        .perform(
            put("/api/v1/matches/{matchId}/circle/membership", matchId)
                .with(user(SECOND_USERNAME))
                .with(csrf()))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    assertThat(rowCount("circle_memberships")).isEqualTo(2);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM circle_memberships WHERE user_id = ?",
                Integer.class,
                SECOND_USER_ID))
        .isOne();

    mockMvc
        .perform(get("/api/v1/matches/{matchId}/circle", matchId).with(user(SECOND_USERNAME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.membershipActive").value(true));
  }

  @Test
  void treatsRepeatedMembershipJoinAsSuccessful() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);

    mockMvc
        .perform(
            put("/api/v1/matches/{matchId}/circle/membership", matchId)
                .with(user(SECOND_USERNAME))
                .with(csrf()))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            put("/api/v1/matches/{matchId}/circle/membership", matchId)
                .with(user(SECOND_USERNAME))
                .with(csrf()))
        .andExpect(status().isNoContent());

    assertThat(rowCount("circle_memberships")).isEqualTo(2);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM circle_memberships WHERE user_id = ?",
                Integer.class,
                SECOND_USER_ID))
        .isOne();
  }

  @Test
  void rejectsAnonymousMembershipJoin() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);

    mockMvc
        .perform(put("/api/v1/matches/{matchId}/circle/membership", matchId).with(csrf()))
        .andExpect(status().isUnauthorized());

    assertThat(rowCount("circle_memberships")).isOne();
  }

  @Test
  @WithMockUser(username = UNMAPPED_USERNAME)
  void rejectsMembershipJoinWhenTheAuthenticatedIdentityCannotBeResolved() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);

    mockMvc
        .perform(put("/api/v1/matches/{matchId}/circle/membership", matchId).with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:authentication-required"))
        .andExpect(jsonPath("$.title").value("Authentication required"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("An authenticated user identity is required"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertThat(rowCount("circle_memberships")).isOne();
  }

  @Test
  void rejectsMembershipJoinWithoutCsrfProtection() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);

    mockMvc
        .perform(
            put("/api/v1/matches/{matchId}/circle/membership", matchId).with(user(SECOND_USERNAME)))
        .andExpect(status().isForbidden());

    assertThat(rowCount("circle_memberships")).isOne();
  }

  @Test
  void rejectsMembershipJoinForAnUnknownMatch() throws Exception {
    UUID unknownMatchId = UUID.fromString("10000000-0000-0000-0000-000000000099");

    mockMvc
        .perform(
            put("/api/v1/matches/{matchId}/circle/membership", unknownMatchId)
                .with(user(SECOND_USERNAME))
                .with(csrf()))
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
  void rejectsMembershipJoinWhenMatchHasNoCircle() throws Exception {
    UUID matchId = storeMatch();

    mockMvc
        .perform(
            put("/api/v1/matches/{matchId}/circle/membership", matchId)
                .with(user(SECOND_USERNAME))
                .with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:circle-not-found"))
        .andExpect(jsonPath("$.title").value("Circle not found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("A circle was not found for match " + matchId))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoCircleWasStored();
  }

  @Test
  void rejectsMembershipJoinWithAMalformedMatchId() throws Exception {
    storeMatch();

    mockMvc
        .perform(
            put("/api/v1/matches/{matchId}/circle/membership", "not-a-uuid")
                .with(user(SECOND_USERNAME))
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-request"))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("The request path contains an invalid value"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoCircleWasStored();
  }

  @Test
  void leavesAnExistingCircleAndReportsInactiveMembership() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);
    joinCircle(matchId, SECOND_USERNAME);

    mockMvc
        .perform(
            delete("/api/v1/matches/{matchId}/circle/membership", matchId)
                .with(user(SECOND_USERNAME))
                .with(csrf()))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    assertThat(rowCount("circles")).isOne();
    assertThat(rowCount("circle_memberships")).isOne();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM circle_memberships WHERE user_id = ?",
                Integer.class,
                SECOND_USER_ID))
        .isZero();

    mockMvc
        .perform(get("/api/v1/matches/{matchId}/circle", matchId).with(user(SECOND_USERNAME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.membershipActive").value(false));
  }

  @Test
  void treatsRepeatedMembershipLeaveAsSuccessful() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);
    joinCircle(matchId, SECOND_USERNAME);

    mockMvc
        .perform(
            delete("/api/v1/matches/{matchId}/circle/membership", matchId)
                .with(user(SECOND_USERNAME))
                .with(csrf()))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            delete("/api/v1/matches/{matchId}/circle/membership", matchId)
                .with(user(SECOND_USERNAME))
                .with(csrf()))
        .andExpect(status().isNoContent());

    assertThat(rowCount("circles")).isOne();
    assertThat(rowCount("circle_memberships")).isOne();
  }

  @Test
  void allowsTheCircleCreatorToLeaveWithoutDeletingTheCircle() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);

    mockMvc
        .perform(
            delete("/api/v1/matches/{matchId}/circle/membership", matchId)
                .with(user(USERNAME))
                .with(csrf()))
        .andExpect(status().isNoContent());

    assertThat(rowCount("circles")).isOne();
    assertThat(rowCount("circle_memberships")).isZero();

    mockMvc
        .perform(get("/api/v1/matches/{matchId}/circle", matchId).with(user(USERNAME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.membershipActive").value(false));
  }

  @Test
  void rejectsAnonymousMembershipLeave() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);

    mockMvc
        .perform(delete("/api/v1/matches/{matchId}/circle/membership", matchId).with(csrf()))
        .andExpect(status().isUnauthorized());

    assertThat(rowCount("circle_memberships")).isOne();
  }

  @Test
  @WithMockUser(username = UNMAPPED_USERNAME)
  void rejectsMembershipLeaveWhenTheAuthenticatedIdentityCannotBeResolved() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);

    mockMvc
        .perform(delete("/api/v1/matches/{matchId}/circle/membership", matchId).with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:authentication-required"))
        .andExpect(jsonPath("$.title").value("Authentication required"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("An authenticated user identity is required"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertThat(rowCount("circle_memberships")).isOne();
  }

  @Test
  void rejectsMembershipLeaveWithoutCsrfProtection() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);

    mockMvc
        .perform(
            delete("/api/v1/matches/{matchId}/circle/membership", matchId).with(user(USERNAME)))
        .andExpect(status().isForbidden());

    assertThat(rowCount("circle_memberships")).isOne();
  }

  @Test
  void rejectsMembershipLeaveForAnUnknownMatch() throws Exception {
    UUID unknownMatchId = UUID.fromString("10000000-0000-0000-0000-000000000099");

    mockMvc
        .perform(
            delete("/api/v1/matches/{matchId}/circle/membership", unknownMatchId)
                .with(user(SECOND_USERNAME))
                .with(csrf()))
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
  void rejectsMembershipLeaveWhenMatchHasNoCircle() throws Exception {
    UUID matchId = storeMatch();

    mockMvc
        .perform(
            delete("/api/v1/matches/{matchId}/circle/membership", matchId)
                .with(user(SECOND_USERNAME))
                .with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:circle-not-found"))
        .andExpect(jsonPath("$.title").value("Circle not found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("A circle was not found for match " + matchId))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoCircleWasStored();
  }

  @Test
  void rejectsMembershipLeaveWithAMalformedMatchId() throws Exception {
    storeMatch();

    mockMvc
        .perform(
            delete("/api/v1/matches/{matchId}/circle/membership", "not-a-uuid")
                .with(user(SECOND_USERNAME))
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-request"))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("The request path contains an invalid value"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoCircleWasStored();
  }

  @Test
  @WithMockUser(username = USERNAME)
  void returnsCircleMetadataWithActiveMembershipForTheCreator() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);

    mockMvc
        .perform(get("/api/v1/matches/{matchId}/circle", matchId))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.matchId").value(matchId.toString()))
        .andExpect(jsonPath("$.createdAt").isNotEmpty())
        .andExpect(jsonPath("$.membershipActive").value(true))
        .andExpect(jsonPath("$.createdByUserId").doesNotExist())
        .andExpect(jsonPath("$.email").doesNotExist());
  }

  @Test
  void returnsCircleMetadataWithInactiveMembershipForANonMember() throws Exception {
    UUID matchId = storeMatch();
    createCircle(matchId, USERNAME);

    mockMvc
        .perform(get("/api/v1/matches/{matchId}/circle", matchId).with(user(SECOND_USERNAME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matchId").value(matchId.toString()))
        .andExpect(jsonPath("$.membershipActive").value(false))
        .andExpect(jsonPath("$.createdByUserId").doesNotExist())
        .andExpect(jsonPath("$.email").doesNotExist());

    assertThat(rowCount("circle_memberships")).isOne();
  }

  @Test
  @WithMockUser(username = USERNAME)
  void rejectsCircleLookupWhenMatchHasNoCircle() throws Exception {
    UUID matchId = storeMatch();

    mockMvc
        .perform(get("/api/v1/matches/{matchId}/circle", matchId))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:circle-not-found"))
        .andExpect(jsonPath("$.title").value("Circle not found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("A circle was not found for match " + matchId))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoCircleWasStored();
  }

  @Test
  @WithMockUser(username = USERNAME)
  void rejectsCircleLookupForAnUnknownMatch() throws Exception {
    UUID unknownMatchId = UUID.fromString("10000000-0000-0000-0000-000000000099");

    mockMvc
        .perform(get("/api/v1/matches/{matchId}/circle", unknownMatchId))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:match-not-found"))
        .andExpect(jsonPath("$.title").value("Match not found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Match " + unknownMatchId + " was not found"));

    assertNoCircleWasStored();
  }

  @Test
  void rejectsAnonymousCircleLookup() throws Exception {
    UUID matchId = storeMatch();

    mockMvc
        .perform(get("/api/v1/matches/{matchId}/circle", matchId))
        .andExpect(status().isUnauthorized());

    assertNoCircleWasStored();
  }

  @Test
  @WithMockUser(username = USERNAME)
  void rejectsCircleLookupWithAMalformedMatchId() throws Exception {
    storeMatch();

    mockMvc
        .perform(get("/api/v1/matches/{matchId}/circle", "not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-request"))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("The request path contains an invalid value"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoCircleWasStored();
  }

  private CreationAttempt createCircleWhenReleased(
      UUID matchId,
      String username,
      UUID userId,
      CountDownLatch requestsReady,
      CountDownLatch startRequests)
      throws Exception {
    requestsReady.countDown();
    if (!startRequests.await(5, TimeUnit.SECONDS)) {
      throw new IllegalStateException("Concurrent circle creation was not started in time");
    }

    int responseStatus =
        mockMvc
            .perform(
                post("/api/v1/matches/{matchId}/circle", matchId).with(user(username)).with(csrf()))
            .andReturn()
            .getResponse()
            .getStatus();
    return new CreationAttempt(userId, responseStatus);
  }

  private void createCircle(UUID matchId, String username) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/matches/{matchId}/circle", matchId).with(user(username)).with(csrf()))
        .andExpect(status().isCreated());
  }

  private void joinCircle(UUID matchId, String username) throws Exception {
    mockMvc
        .perform(
            put("/api/v1/matches/{matchId}/circle/membership", matchId)
                .with(user(username))
                .with(csrf()))
        .andExpect(status().isNoContent());
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
    CurrentUser testCurrentUser() {
      Map<String, UUID> userIds = Map.of(USERNAME, USER_ID, SECOND_USERNAME, SECOND_USER_ID);
      return () -> {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
          return Optional.empty();
        }
        return Optional.ofNullable(userIds.get(authentication.getName()));
      };
    }
  }

  private record CreationAttempt(UUID userId, int status) {}
}

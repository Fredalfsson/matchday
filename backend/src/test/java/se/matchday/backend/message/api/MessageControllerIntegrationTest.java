package se.matchday.backend.message.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import se.matchday.backend.TestcontainersConfiguration;
import se.matchday.backend.circle.application.CircleRepository;
import se.matchday.backend.circle.domain.Circle;
import se.matchday.backend.identity.application.CurrentUser;
import se.matchday.backend.identity.application.UserDirectory;
import se.matchday.backend.match.application.MatchRepository;
import se.matchday.backend.match.application.ProviderMatch;
import se.matchday.backend.match.domain.MatchStatus;

@Import({
  TestcontainersConfiguration.class,
  MessageControllerIntegrationTest.TestIdentityConfiguration.class
})
@AutoConfigureMockMvc
@SpringBootTest
class MessageControllerIntegrationTest {

  private static final String MEMBER_USERNAME = "message-member";
  private static final String NON_MEMBER_USERNAME = "message-non-member";
  private static final String DIRECTORY_UNAVAILABLE_USERNAME = "message-directory-unavailable";
  private static final String UNMAPPED_USERNAME = "message-unmapped-user";
  private static final UUID MEMBER_USER_ID =
      UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID NON_MEMBER_USER_ID =
      UUID.fromString("30000000-0000-0000-0000-000000000002");
  private static final UUID DIRECTORY_UNAVAILABLE_USER_ID =
      UUID.fromString("30000000-0000-0000-0000-000000000003");
  private static final Instant CREATED_AT = Instant.parse("2026-10-04T08:00:00Z");

  private final MockMvc mockMvc;
  private final MatchRepository matchRepository;
  private final CircleRepository circleRepository;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  MessageControllerIntegrationTest(
      MockMvc mockMvc,
      MatchRepository matchRepository,
      CircleRepository circleRepository,
      JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.matchRepository = matchRepository;
    this.circleRepository = circleRepository;
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

  @Test
  void createsAMessageForAnActiveCircleMember() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/circles/{circleId}/messages", circle.id())
                    .with(user(MEMBER_USERNAME))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"content\":\"  Första raden\\nAndra raden  \"}"))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.circleId").value(circle.id().toString()))
            .andExpect(jsonPath("$.content").value("Första raden\nAndra raden"))
            .andExpect(jsonPath("$.authorUsername").value("sara"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.authorUserId").doesNotExist())
            .andExpect(jsonPath("$.email").doesNotExist())
            .andReturn();

    String messageId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    assertThat(result.getResponse().getHeader("Location"))
        .isEqualTo("/api/v1/circles/" + circle.id() + "/messages/" + messageId);

    assertThat(rowCount("messages")).isOne();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE circle_id = ? AND author_user_id = ? AND content = ?",
                Integer.class,
                circle.id(),
                MEMBER_USER_ID,
                "Första raden\nAndra raden"))
        .isOne();
  }

  @Test
  void rejectsAnonymousMessageCreation() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"content":"Hej"}
                    """))
        .andExpect(status().isUnauthorized());

    assertNoMessageWasStored();
  }

  @Test
  void rejectsMessageCreationWhenTheAuthenticatedIdentityCannotBeResolved() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(UNMAPPED_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"content":"Hej"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:authentication-required"))
        .andExpect(jsonPath("$.title").value("Authentication required"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("An authenticated user identity is required"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void rejectsMessageCreationWithAMalformedCircleId() throws Exception {
    storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", "not-a-uuid")
                .with(user(MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"content":"Hej"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-request"))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("The request path contains an invalid value"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void rejectsMessageCreationWithoutARequestBody() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-request"))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("The request body is missing or malformed"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void rejectsMessageCreationWithMalformedJson() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-request"))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("The request body is missing or malformed"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void rejectsMessageCreationWithoutContent() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-message-content"))
        .andExpect(jsonPath("$.title").value("Invalid message content"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void rejectsNullMessageContent() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"content":null}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-message-content"))
        .andExpect(jsonPath("$.title").value("Invalid message content"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void rejectsMessageCreationForAnInactiveMember() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(NON_MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"content":"Hej"}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(
            jsonPath("$.type").value("urn:matchday:problem:active-circle-membership-required"))
        .andExpect(jsonPath("$.title").value("Active circle membership required"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(
            jsonPath("$.detail")
                .value("An active membership is required for circle " + circle.id()))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void rejectsMessageCreationForAnUnknownCircle() throws Exception {
    UUID unknownCircleId = UUID.fromString("40000000-0000-0000-0000-000000000099");

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", unknownCircleId)
                .with(user(MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"content":"Hej"}
                    """))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:circle-not-found"))
        .andExpect(jsonPath("$.title").value("Circle not found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Circle " + unknownCircleId + " was not found"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void rejectsBlankMessageContent() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"content":"   "}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-message-content"))
        .andExpect(jsonPath("$.title").value("Invalid message content"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void rejectsMessageContentLongerThanOneThousandUnicodeCharacters() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);
    String content = "😀".repeat(1_001);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + content + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-message-content"))
        .andExpect(jsonPath("$.title").value("Invalid message content"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void acceptsMessageContentWithExactlyOneThousandUnicodeCharacters() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);
    String content = "😀".repeat(1_000);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + content + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value(content));

    assertThat(rowCount("messages")).isOne();
  }

  @Test
  void rejectsNullCharactersInMessageContent() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Hej\\u0000\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:invalid-message-content"))
        .andExpect(jsonPath("$.title").value("Invalid message content"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void createsDistinctMessagesForRepeatedRequests() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);
    String requestBody = "{\"content\":\"Samma innehåll\"}";

    String firstLocation =
        mockMvc
            .perform(
                post("/api/v1/circles/{circleId}/messages", circle.id())
                    .with(user(MEMBER_USERNAME))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");
    String secondLocation =
        mockMvc
            .perform(
                post("/api/v1/circles/{circleId}/messages", circle.id())
                    .with(user(MEMBER_USERNAME))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(firstLocation).isNotNull().isNotEqualTo(secondLocation);
    assertThat(rowCount("messages")).isEqualTo(2);
  }

  @Test
  void ignoresClientSuppliedServerOwnedMessageFields() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(MEMBER_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content":"Hej",
                      "authorUserId":"99999999-9999-9999-9999-999999999999",
                      "authorUsername":"attacker",
                      "createdAt":"2000-01-01T00:00:00Z"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.authorUsername").value("sara"))
        .andExpect(jsonPath("$.createdAt").value(not("2000-01-01T00:00:00Z")))
        .andExpect(jsonPath("$.authorUserId").doesNotExist());

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE author_user_id = ?",
                Integer.class,
                MEMBER_USER_ID))
        .isOne();
  }

  @Test
  void rejectsMessageCreationWhenTheUserDirectoryIsUnavailable() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);
    circleRepository.addMembershipIfAbsent(
        circle.id(), DIRECTORY_UNAVAILABLE_USER_ID, CREATED_AT.plusSeconds(1));

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(DIRECTORY_UNAVAILABLE_USERNAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"content":"Hej"}
                    """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:matchday:problem:user-directory-unavailable"))
        .andExpect(jsonPath("$.title").value("User directory unavailable"))
        .andExpect(jsonPath("$.status").value(503))
        .andExpect(jsonPath("$.detail").value("The public user directory is unavailable"))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    assertNoMessageWasStored();
  }

  @Test
  void rejectsMessageCreationWithoutCsrfProtection() throws Exception {
    Circle circle = storeCircleWithCreator(MEMBER_USER_ID);

    mockMvc
        .perform(
            post("/api/v1/circles/{circleId}/messages", circle.id())
                .with(user(MEMBER_USERNAME))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"content":"Hej"}
                    """))
        .andExpect(status().isForbidden());

    assertNoMessageWasStored();
  }

  private Circle storeCircleWithCreator(UUID creatorUserId) {
    UUID matchId = storeMatch();
    return circleRepository.createWithCreatorMembership(matchId, creatorUserId, CREATED_AT);
  }

  private UUID storeMatch() {
    matchRepository.saveAll(
        List.of(
            new ProviderMatch(
                "message-controller-test-match",
                2026,
                1,
                "home-1",
                "Home",
                "away-1",
                "Away",
                LocalDate.of(2026, 10, 4),
                Instant.parse("2026-10-04T13:00:00Z"),
                null,
                null,
                MatchStatus.SCHEDULED,
                null)));
    return matchRepository.findAll().getFirst().id();
  }

  private void assertNoMessageWasStored() {
    assertThat(rowCount("messages")).isZero();
  }

  private int rowCount(String table) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
  }

  private void clearDatabase() {
    jdbcTemplate.update("TRUNCATE TABLE messages, circle_memberships, circles, matches");
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestIdentityConfiguration {

    @Bean
    @Primary
    CurrentUser testCurrentUser() {
      Map<String, UUID> userIds =
          Map.of(
              MEMBER_USERNAME,
              MEMBER_USER_ID,
              NON_MEMBER_USERNAME,
              NON_MEMBER_USER_ID,
              DIRECTORY_UNAVAILABLE_USERNAME,
              DIRECTORY_UNAVAILABLE_USER_ID);
      return () -> {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
          return Optional.empty();
        }
        return Optional.ofNullable(userIds.get(authentication.getName()));
      };
    }

    @Bean
    @Primary
    UserDirectory testUserDirectory() {
      return userIds ->
          userIds.contains(MEMBER_USER_ID) ? Map.of(MEMBER_USER_ID, "sara") : Map.of();
    }
  }
}

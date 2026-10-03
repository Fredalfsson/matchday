package se.matchday.backend.message.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
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
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import se.matchday.backend.TestcontainersConfiguration;
import se.matchday.backend.circle.application.CircleRepository;
import se.matchday.backend.circle.domain.Circle;
import se.matchday.backend.match.application.MatchRepository;
import se.matchday.backend.match.application.ProviderMatch;
import se.matchday.backend.match.domain.MatchStatus;
import se.matchday.backend.message.application.MessageRepository;
import se.matchday.backend.message.domain.Message;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JpaMessageRepositoryAdapterIntegrationTest {

  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final Instant CREATED_AT = Instant.parse("2026-10-02T18:30:00Z");

  private final MessageRepository messageRepository;
  private final SpringDataMessageJpaRepository messageJpaRepository;
  private final CircleRepository circleRepository;
  private final MatchRepository matchRepository;
  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;

  @Autowired
  JpaMessageRepositoryAdapterIntegrationTest(
      MessageRepository messageRepository,
      SpringDataMessageJpaRepository messageJpaRepository,
      CircleRepository circleRepository,
      MatchRepository matchRepository,
      JdbcTemplate jdbcTemplate,
      PlatformTransactionManager transactionManager) {
    this.messageRepository = messageRepository;
    this.messageJpaRepository = messageJpaRepository;
    this.circleRepository = circleRepository;
    this.matchRepository = matchRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @BeforeEach
  void clearDatabaseBeforeTest() {
    clearDatabase();
  }

  @AfterEach
  void clearDatabaseAfterTest() {
    clearDatabase();
  }

  private void clearDatabase() {
    jdbcTemplate.update("TRUNCATE TABLE messages, circle_memberships, circles, matches");
  }

  @Test
  void persistsAMemberAuthoredMessage() {
    Circle circle = storeCircle();

    Message message =
        messageRepository
            .createIfActiveMember(circle.id(), USER_ID, "First line\nSecond line", CREATED_AT)
            .orElseThrow();
    Message storedMessage = messageJpaRepository.findById(message.id()).orElseThrow().toDomain();

    assertThat(message.id()).isNotNull();
    assertThat(message.circleId()).isEqualTo(circle.id());
    assertThat(message.authorUserId()).isEqualTo(USER_ID);
    assertThat(message.content()).isEqualTo("First line\nSecond line");
    assertThat(message.createdAt()).isEqualTo(CREATED_AT);
    assertThat(storedMessage).isEqualTo(message);
  }

  @Test
  void persistsRepeatedContentAsDistinctMessages() {
    Circle circle = storeCircle();

    Message first =
        messageRepository
            .createIfActiveMember(circle.id(), USER_ID, "Same content", CREATED_AT)
            .orElseThrow();
    Message second =
        messageRepository
            .createIfActiveMember(circle.id(), USER_ID, "Same content", CREATED_AT.plusSeconds(1))
            .orElseThrow();

    assertThat(first.id()).isNotEqualTo(second.id());
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void keepsMessageHistoryWhenTheAuthorLeavesTheCircle() {
    Circle circle = storeCircle();
    Message message =
        messageRepository
            .createIfActiveMember(circle.id(), USER_ID, "Persistent history", CREATED_AT)
            .orElseThrow();

    circleRepository.removeMembershipIfPresent(circle.id(), USER_ID);

    assertThat(circleRepository.hasActiveMembership(circle.id(), USER_ID)).isFalse();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE id = ?", Integer.class, message.id()))
        .isOne();
  }

  @Test
  void doesNotPersistAMessageForANonMember() {
    Circle circle = storeCircle();
    UUID nonMemberUserId = UUID.randomUUID();

    Optional<Message> result =
        messageRepository.createIfActiveMember(
            circle.id(), nonMemberUserId, "Not allowed", CREATED_AT);

    assertThat(result).isEmpty();
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Integer.class))
        .isZero();
  }

  @Test
  void doesNotPersistAMessageAfterTheAuthorLeavesTheCircle() {
    Circle circle = storeCircle();
    circleRepository.removeMembershipIfPresent(circle.id(), USER_ID);

    Optional<Message> result =
        messageRepository.createIfActiveMember(circle.id(), USER_ID, "Too late", CREATED_AT);

    assertThat(result).isEmpty();
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Integer.class))
        .isZero();
  }

  @Test
  void doesNotPersistAMessageWhenAConcurrentLeaveCommitsFirst() throws Exception {
    Circle circle = storeCircle();
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch membershipDeleted = new CountDownLatch(1);
    CountDownLatch allowLeaveCommit = new CountDownLatch(1);

    try {
      Future<?> leaveAttempt =
          executor.submit(
              () ->
                  deleteMembershipAndAwaitCommit(circle.id(), membershipDeleted, allowLeaveCommit));
      assertThat(membershipDeleted.await(5, TimeUnit.SECONDS)).isTrue();

      Future<Optional<Message>> createAttempt =
          executor.submit(
              () ->
                  messageRepository.createIfActiveMember(
                      circle.id(), USER_ID, "Concurrent message", CREATED_AT));
      awaitMessageInsertBlockedByMembershipLock();

      allowLeaveCommit.countDown();
      leaveAttempt.get(10, TimeUnit.SECONDS);

      assertThat(createAttempt.get(10, TimeUnit.SECONDS)).isEmpty();
      assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Integer.class))
          .isZero();
    } finally {
      allowLeaveCommit.countDown();
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void persistsOneThousandUnicodeCharactersAtTheDatabaseBoundary() {
    Circle circle = storeCircle();
    String content = "😀".repeat(1_000);

    Message message =
        messageRepository
            .createIfActiveMember(circle.id(), USER_ID, content, CREATED_AT)
            .orElseThrow();

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT char_length(content) FROM messages WHERE id = ?",
                Integer.class,
                message.id()))
        .isEqualTo(1_000);
  }

  @Test
  void rejectsAMessageForAnUnknownCircleAtTheDatabaseBoundary() {
    UUID unknownCircleId = UUID.randomUUID();

    assertThatThrownBy(() -> insertMessageDirectly(unknownCircleId, "Unknown circle"))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Integer.class))
        .isZero();
  }

  @Test
  void rejectsEmptyContentAtTheDatabaseBoundary() {
    Circle circle = storeCircle();

    assertThatThrownBy(() -> insertMessageDirectly(circle.id(), ""))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Integer.class))
        .isZero();
  }

  @Test
  void rejectsContentLongerThanOneThousandCharactersAtTheDatabaseBoundary() {
    Circle circle = storeCircle();

    assertThatThrownBy(() -> insertMessageDirectly(circle.id(), "a".repeat(1_001)))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Integer.class))
        .isZero();
  }

  private void deleteMembershipAndAwaitCommit(
      UUID circleId, CountDownLatch membershipDeleted, CountDownLatch allowLeaveCommit) {
    transactionTemplate.executeWithoutResult(
        transactionStatus -> {
          jdbcTemplate.update(
              "DELETE FROM circle_memberships WHERE circle_id = ? AND user_id = ?",
              circleId,
              USER_ID);
          membershipDeleted.countDown();
          await(allowLeaveCommit, "Concurrent leave was not released in time");
        });
  }

  private void awaitMessageInsertBlockedByMembershipLock() {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < deadline) {
      Long waitingStatements =
          jdbcTemplate.queryForObject(
              """
              SELECT COUNT(*)
              FROM pg_stat_activity
              WHERE datname = current_database()
                AND position('INSERT INTO messages' IN query) > 0
                AND wait_event_type = 'Lock'
              """,
              Long.class);
      if (waitingStatements != null && waitingStatements > 0) {
        return;
      }
      try {
        Thread.sleep(25);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(
            "Interrupted while waiting for the message insert to acquire its lock", exception);
      }
    }
    throw new AssertionError("Message insert did not wait for the membership lock");
  }

  private static void await(CountDownLatch latch, String timeoutMessage) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException(timeoutMessage);
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Concurrent message test was interrupted", exception);
    }
  }

  private void insertMessageDirectly(UUID circleId, String content) {
    jdbcTemplate.update(
        """
        INSERT INTO messages (id, circle_id, author_user_id, content, created_at)
        VALUES (?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        circleId,
        USER_ID,
        content,
        OffsetDateTime.ofInstant(CREATED_AT, java.time.ZoneOffset.UTC));
  }

  private Circle storeCircle() {
    matchRepository.saveAll(
        List.of(
            new ProviderMatch(
                "persistence-message-match",
                2026,
                1,
                "home-1",
                "Home",
                "away-1",
                "Away",
                LocalDate.of(2026, 4, 4),
                null,
                null,
                null,
                MatchStatus.SCHEDULED,
                null)));
    UUID matchId = matchRepository.findAll().getFirst().id();
    return circleRepository.createWithCreatorMembership(
        matchId, USER_ID, CREATED_AT.minusSeconds(1));
  }
}

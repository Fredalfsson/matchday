package se.matchday.backend.circle.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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
import se.matchday.backend.TestcontainersConfiguration;
import se.matchday.backend.circle.application.CircleAlreadyExistsException;
import se.matchday.backend.circle.application.CircleRepository;
import se.matchday.backend.circle.domain.Circle;
import se.matchday.backend.match.application.MatchRepository;
import se.matchday.backend.match.application.ProviderMatch;
import se.matchday.backend.match.domain.MatchStatus;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JpaCircleRepositoryAdapterIntegrationTest {

  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID SECOND_USER_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000002");
  private static final Instant CREATED_AT = Instant.parse("2026-09-27T10:15:30Z");

  private final CircleRepository circleRepository;
  private final MatchRepository matchRepository;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  JpaCircleRepositoryAdapterIntegrationTest(
      CircleRepository circleRepository,
      MatchRepository matchRepository,
      JdbcTemplate jdbcTemplate) {
    this.circleRepository = circleRepository;
    this.matchRepository = matchRepository;
    this.jdbcTemplate = jdbcTemplate;
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
  void persistsCircleAndCreatorMembershipInOneOperation() {
    UUID matchId = storeMatch();

    Circle circle = circleRepository.createWithCreatorMembership(matchId, USER_ID, CREATED_AT);

    assertThat(circle.matchId()).isEqualTo(matchId);
    assertThat(circle.createdByUserId()).isEqualTo(USER_ID);
    assertThat(circle.createdAt()).isEqualTo(CREATED_AT);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT user_id FROM circle_memberships WHERE circle_id = ?",
                UUID.class,
                circle.id()))
        .isEqualTo(USER_ID);
  }

  @Test
  void findsCircleByMatchAndReportsMembershipStatus() {
    UUID matchId = storeMatch();
    Circle created = circleRepository.createWithCreatorMembership(matchId, USER_ID, CREATED_AT);

    assertThat(circleRepository.findByMatchId(matchId)).contains(created);
    assertThat(circleRepository.hasActiveMembership(created.id(), USER_ID)).isTrue();
    assertThat(circleRepository.hasActiveMembership(created.id(), UUID.randomUUID())).isFalse();
  }

  @Test
  void returnsEmptyWhenAnExistingMatchHasNoCircle() {
    UUID matchId = storeMatch();

    assertThat(circleRepository.findByMatchId(matchId)).isEmpty();
  }

  @Test
  void addsMembershipForANonMember() {
    UUID matchId = storeMatch();
    Circle circle = circleRepository.createWithCreatorMembership(matchId, USER_ID, CREATED_AT);
    Instant joinedAt = CREATED_AT.plusSeconds(1);

    circleRepository.addMembershipIfAbsent(circle.id(), SECOND_USER_ID, joinedAt);

    assertThat(circleRepository.hasActiveMembership(circle.id(), SECOND_USER_ID)).isTrue();
    assertThat(membershipCount(circle.id(), SECOND_USER_ID)).isOne();
    assertThat(membershipCount(circle.id(), SECOND_USER_ID, joinedAt)).isOne();
  }

  @Test
  void keepsAnExistingMembershipUnchanged() {
    UUID matchId = storeMatch();
    Circle circle = circleRepository.createWithCreatorMembership(matchId, USER_ID, CREATED_AT);
    Instant joinedAt = CREATED_AT.plusSeconds(1);

    circleRepository.addMembershipIfAbsent(circle.id(), SECOND_USER_ID, joinedAt);
    circleRepository.addMembershipIfAbsent(circle.id(), SECOND_USER_ID, joinedAt.plusSeconds(1));

    assertThat(membershipCount(circle.id(), SECOND_USER_ID)).isOne();
    assertThat(membershipCount(circle.id(), SECOND_USER_ID, joinedAt)).isOne();
  }

  @Test
  void allowsConcurrentIdempotentMembershipJoins() throws Exception {
    UUID matchId = storeMatch();
    Circle circle = circleRepository.createWithCreatorMembership(matchId, USER_ID, CREATED_AT);
    Instant joinedAt = CREATED_AT.plusSeconds(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch requestsReady = new CountDownLatch(2);
    CountDownLatch startRequests = new CountDownLatch(1);

    try {
      Future<?> firstAttempt =
          executor.submit(
              () -> joinWhenReleased(circle.id(), joinedAt, requestsReady, startRequests));
      Future<?> secondAttempt =
          executor.submit(
              () -> joinWhenReleased(circle.id(), joinedAt, requestsReady, startRequests));

      assertThat(requestsReady.await(5, TimeUnit.SECONDS)).isTrue();
      startRequests.countDown();
      firstAttempt.get(10, TimeUnit.SECONDS);
      secondAttempt.get(10, TimeUnit.SECONDS);

      assertThat(membershipCount(circle.id(), SECOND_USER_ID)).isOne();
    } finally {
      startRequests.countDown();
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void removesAnExistingMembershipWithoutAffectingOtherMembers() {
    UUID matchId = storeMatch();
    Circle circle = circleRepository.createWithCreatorMembership(matchId, USER_ID, CREATED_AT);
    circleRepository.addMembershipIfAbsent(circle.id(), SECOND_USER_ID, CREATED_AT.plusSeconds(1));

    circleRepository.removeMembershipIfPresent(circle.id(), SECOND_USER_ID);

    assertThat(circleRepository.hasActiveMembership(circle.id(), SECOND_USER_ID)).isFalse();
    assertThat(membershipCount(circle.id(), SECOND_USER_ID)).isZero();
    assertThat(circleRepository.hasActiveMembership(circle.id(), USER_ID)).isTrue();
    assertThat(membershipCount(circle.id(), USER_ID)).isOne();
  }

  @Test
  void ignoresAnAbsentMembership() {
    UUID matchId = storeMatch();
    Circle circle = circleRepository.createWithCreatorMembership(matchId, USER_ID, CREATED_AT);

    circleRepository.removeMembershipIfPresent(circle.id(), SECOND_USER_ID);

    assertThat(circleRepository.hasActiveMembership(circle.id(), USER_ID)).isTrue();
    assertThat(membershipCount(circle.id(), USER_ID)).isOne();
  }

  @Test
  void allowsAFormerMemberToJoinAgainWithANewTimestamp() {
    UUID matchId = storeMatch();
    Circle circle = circleRepository.createWithCreatorMembership(matchId, USER_ID, CREATED_AT);
    Instant firstJoinedAt = CREATED_AT.plusSeconds(1);
    Instant secondJoinedAt = CREATED_AT.plusSeconds(2);
    circleRepository.addMembershipIfAbsent(circle.id(), SECOND_USER_ID, firstJoinedAt);

    circleRepository.removeMembershipIfPresent(circle.id(), SECOND_USER_ID);
    circleRepository.addMembershipIfAbsent(circle.id(), SECOND_USER_ID, secondJoinedAt);

    assertThat(membershipCount(circle.id(), SECOND_USER_ID)).isOne();
    assertThat(membershipCount(circle.id(), SECOND_USER_ID, firstJoinedAt)).isZero();
    assertThat(membershipCount(circle.id(), SECOND_USER_ID, secondJoinedAt)).isOne();
  }

  @Test
  void mapsTheOneCirclePerMatchConstraintToADomainConflict() {
    UUID matchId = storeMatch();
    circleRepository.createWithCreatorMembership(matchId, USER_ID, CREATED_AT);

    assertThatThrownBy(
            () ->
                circleRepository.createWithCreatorMembership(
                    matchId, UUID.randomUUID(), CREATED_AT.plusSeconds(1)))
        .isInstanceOf(CircleAlreadyExistsException.class)
        .hasMessage("A circle already exists for match " + matchId);

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM circles", Integer.class)).isOne();
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM circle_memberships", Integer.class))
        .isOne();
  }

  @Test
  void rollsBackTheCircleWhenCreatorMembershipCannotBePersisted() {
    UUID matchId = storeMatch();
    jdbcTemplate.execute(
        """
        ALTER TABLE circle_memberships
        ADD CONSTRAINT test_reject_creator_membership
        CHECK (user_id <> '20000000-0000-0000-0000-000000000001')
        """);

    try {
      assertThatThrownBy(
              () -> circleRepository.createWithCreatorMembership(matchId, USER_ID, CREATED_AT))
          .isInstanceOf(DataIntegrityViolationException.class);
    } finally {
      jdbcTemplate.execute(
          """
          ALTER TABLE circle_memberships
          DROP CONSTRAINT test_reject_creator_membership
          """);
    }

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM circles", Integer.class)).isZero();
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM circle_memberships", Integer.class))
        .isZero();
  }

  @Test
  void doesNotMapAnUnrelatedDatabaseConstraintToACircleConflict() {
    UUID matchId = storeMatch();
    jdbcTemplate.execute(
        """
        ALTER TABLE circles
        ADD CONSTRAINT test_reject_circle_creator
        CHECK (created_by_user_id <> '20000000-0000-0000-0000-000000000001')
        """);

    try {
      assertThatThrownBy(
              () -> circleRepository.createWithCreatorMembership(matchId, USER_ID, CREATED_AT))
          .isInstanceOf(DataIntegrityViolationException.class)
          .isNotInstanceOf(CircleAlreadyExistsException.class);
    } finally {
      jdbcTemplate.execute(
          """
          ALTER TABLE circles
          DROP CONSTRAINT test_reject_circle_creator
          """);
    }

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM circles", Integer.class)).isZero();
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM circle_memberships", Integer.class))
        .isZero();
  }

  private void joinWhenReleased(
      UUID circleId, Instant joinedAt, CountDownLatch requestsReady, CountDownLatch startRequests) {
    requestsReady.countDown();
    try {
      if (!startRequests.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Concurrent membership joins were not started in time");
      }
      circleRepository.addMembershipIfAbsent(circleId, SECOND_USER_ID, joinedAt);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Concurrent membership join was interrupted", exception);
    }
  }

  private int membershipCount(UUID circleId, UUID userId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM circle_memberships WHERE circle_id = ? AND user_id = ?",
        Integer.class,
        circleId,
        userId);
  }

  private int membershipCount(UUID circleId, UUID userId, Instant joinedAt) {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM circle_memberships
        WHERE circle_id = ? AND user_id = ? AND joined_at = ?
        """,
        Integer.class,
        circleId,
        userId,
        joinedAt.atOffset(ZoneOffset.UTC));
  }

  private UUID storeMatch() {
    matchRepository.saveAll(
        List.of(
            new ProviderMatch(
                "persistence-circle-match",
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
    return matchRepository.findAll().getFirst().id();
  }
}

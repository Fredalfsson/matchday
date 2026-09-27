package se.matchday.backend.circle.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
    jdbcTemplate.update("TRUNCATE TABLE circle_memberships, circles, matches");
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

package se.matchday.backend.match.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import se.matchday.backend.TestcontainersConfiguration;
import se.matchday.backend.match.application.MatchRepository;
import se.matchday.backend.match.domain.Match;
import se.matchday.backend.match.domain.MatchStatus;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JpaMatchRepositoryAdapterIntegrationTest {

  private final MatchRepository repository;

  @Autowired
  JpaMatchRepositoryAdapterIntegrationTest(MatchRepository repository) {
    this.repository = repository;
  }

  @Test
  void insertsNewMatchesAndUpdatesAnExistingMatchByExternalId() {
    Match scheduled = scheduledMatch("event-1", 1, LocalDate.of(2026, 4, 4));
    repository.saveAll(List.of(scheduled));

    Match finished =
        new Match(
            "event-1",
            2026,
            1,
            "home-1",
            "Home",
            "away-1",
            "Away",
            LocalDate.of(2026, 4, 5),
            Instant.parse("2026-04-05T13:00:00Z"),
            2,
            1,
            MatchStatus.FINISHED,
            "Updated Arena");
    Match second = scheduledMatch("event-2", 2, LocalDate.of(2026, 4, 12));
    repository.saveAll(List.of(finished, second));

    assertThat(repository.findAll()).containsExactlyInAnyOrder(finished, second);
  }

  private Match scheduledMatch(String externalId, int round, LocalDate scheduledDate) {
    return new Match(
        externalId,
        2026,
        round,
        "home-" + round,
        "Home",
        "away-" + round,
        "Away",
        scheduledDate,
        Instant.parse("2026-04-04T13:00:00Z").plusSeconds((round - 1L) * 604_800),
        null,
        null,
        MatchStatus.SCHEDULED,
        null);
  }
}

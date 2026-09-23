package se.matchday.backend.match.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import se.matchday.backend.match.domain.Match;
import se.matchday.backend.match.domain.MatchStatus;

class MatchQueryServiceTest {

  @Test
  void listsMatchesInTheDefaultApiOrder() {
    Match previousSeason =
        match("00000000-0000-0000-0000-000000000001", 2025, 30, "2025-11-09", null);
    Match earlierDate = match("00000000-0000-0000-0000-000000000007", 2026, 1, "2026-04-03", null);
    Match earlierKickoff =
        match(
            "00000000-0000-0000-0000-000000000002", 2026, 1, "2026-04-04", "2026-04-04T13:00:00Z");
    Match sameKickoffLowerId =
        match(
            "00000000-0000-0000-0000-000000000003", 2026, 1, "2026-04-04", "2026-04-04T15:00:00Z");
    Match sameKickoffHigherId =
        match(
            "00000000-0000-0000-0000-000000000004", 2026, 1, "2026-04-04", "2026-04-04T15:00:00Z");
    Match unknownKickoff =
        match("00000000-0000-0000-0000-000000000005", 2026, 1, "2026-04-04", null);
    Match nextRound =
        match(
            "00000000-0000-0000-0000-000000000006", 2026, 2, "2026-04-01", "2026-04-01T13:00:00Z");

    MatchRepository repository =
        new StubMatchRepository(
            List.of(
                unknownKickoff,
                nextRound,
                earlierDate,
                sameKickoffHigherId,
                earlierKickoff,
                previousSeason,
                sameKickoffLowerId));

    List<Match> matches = new MatchQueryService(repository).listMatches();

    assertThat(matches)
        .extracting(match -> match.id())
        .containsExactly(
            previousSeason.id(),
            earlierDate.id(),
            earlierKickoff.id(),
            sameKickoffLowerId.id(),
            sameKickoffHigherId.id(),
            unknownKickoff.id(),
            nextRound.id());
  }

  private Match match(
      String id, int season, int round, String scheduledDate, @Nullable String kickoffAt) {
    return new Match(
        UUID.fromString(id),
        season,
        round,
        "Home",
        "Away",
        LocalDate.parse(scheduledDate),
        kickoffAt == null ? null : Instant.parse(kickoffAt),
        null,
        null,
        MatchStatus.SCHEDULED,
        null);
  }

  private record StubMatchRepository(List<Match> matches) implements MatchRepository {

    @Override
    public void saveAll(List<ProviderMatch> matches) {
      throw new UnsupportedOperationException("not used by this test");
    }

    @Override
    public List<Match> findAll() {
      return matches;
    }
  }
}

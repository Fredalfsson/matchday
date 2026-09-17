package se.matchday.backend.match.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import se.matchday.backend.match.domain.Match;
import se.matchday.backend.match.domain.MatchStatus;

class SeasonMatchImporterTest {

  @Test
  void importsEveryRoundInOrderAndCollectsTheMatches() {
    RecordingMatchDataProvider provider = new RecordingMatchDataProvider();
    RecordingMatchRepository repository = new RecordingMatchRepository();
    SeasonMatchImporter importer = new SeasonMatchImporter(provider, repository);

    List<ProviderMatch> matches = importer.importSeason(2026);

    assertThat(provider.requestedRounds()).containsExactlyElementsOf(roundsOneThroughThirty());
    assertThat(matches)
        .hasSize(240)
        .allSatisfy(match -> assertThat(match.season()).isEqualTo(2026));
    assertThat(matches).filteredOn(match -> match.round() == 1).hasSize(8);
    assertThat(matches).filteredOn(match -> match.round() == 30).hasSize(8);
    assertThat(repository.savedMatches()).containsExactlyElementsOf(matches);
  }

  @Test
  void rejectsAnInvalidSeasonBeforeCallingTheProvider() {
    RecordingMatchDataProvider provider = new RecordingMatchDataProvider();
    RecordingMatchRepository repository = new RecordingMatchRepository();
    SeasonMatchImporter importer = new SeasonMatchImporter(provider, repository);

    assertThatThrownBy(() -> importer.importSeason(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("season must be a positive integer");
    assertThat(provider.requestedRounds()).isEmpty();
    assertThat(repository.savedMatches()).isEmpty();
  }

  @Test
  void stopsAtTheFailingRoundAndPropagatesTheFailure() {
    List<Integer> requestedRounds = new ArrayList<>();
    IllegalStateException failure = new IllegalStateException("provider unavailable");
    MatchDataProvider provider =
        (season, round) -> {
          requestedRounds.add(round);
          if (round == 3) {
            throw failure;
          }
          return List.of();
        };
    RecordingMatchRepository repository = new RecordingMatchRepository();
    SeasonMatchImporter importer = new SeasonMatchImporter(provider, repository);

    assertThatThrownBy(() -> importer.importSeason(2026)).isSameAs(failure);
    assertThat(requestedRounds).containsExactly(1, 2, 3);
    assertThat(repository.savedMatches()).isEmpty();
  }

  private List<Integer> roundsOneThroughThirty() {
    return IntStream.rangeClosed(1, 30).boxed().toList();
  }

  private static final class RecordingMatchDataProvider implements MatchDataProvider {

    private final List<Integer> requestedRounds = new ArrayList<>();

    @Override
    public List<ProviderMatch> fetchRound(int season, int round) {
      requestedRounds.add(round);
      return IntStream.rangeClosed(1, 8)
          .mapToObj(matchNumber -> match(season, round, matchNumber))
          .toList();
    }

    private List<Integer> requestedRounds() {
      return List.copyOf(requestedRounds);
    }

    private ProviderMatch match(int season, int round, int matchNumber) {
      return new ProviderMatch(
          "event-" + round + "-" + matchNumber,
          season,
          round,
          "home-" + round + "-" + matchNumber,
          "Home " + round + "-" + matchNumber,
          "away-" + round + "-" + matchNumber,
          "Away " + round + "-" + matchNumber,
          LocalDate.of(season, 1, 1).plusDays(round - 1L),
          null,
          null,
          null,
          MatchStatus.SCHEDULED,
          null);
    }
  }

  private static final class RecordingMatchRepository implements MatchRepository {

    private List<ProviderMatch> matches = List.of();

    @Override
    public void saveAll(List<ProviderMatch> matches) {
      this.matches = List.copyOf(matches);
    }

    @Override
    public List<Match> findAll() {
      return List.of();
    }

    List<ProviderMatch> savedMatches() {
      return matches;
    }
  }
}

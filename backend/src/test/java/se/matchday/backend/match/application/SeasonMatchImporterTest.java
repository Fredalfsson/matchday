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

    SeasonMatchImportResult result = importer.importSeason(2026);
    List<ProviderMatch> savedMatches = repository.savedMatches();

    assertThat(provider.requestedRounds()).containsExactlyElementsOf(roundsOneThroughThirty());
    assertThat(result).isEqualTo(new SeasonMatchImportResult(2026, 240));
    assertThat(savedMatches)
        .hasSize(240)
        .allSatisfy(match -> assertThat(match.season()).isEqualTo(2026));
    assertThat(savedMatches).filteredOn(match -> match.round() == 1).hasSize(8);
    assertThat(savedMatches).filteredOn(match -> match.round() == 30).hasSize(8);
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
          return validRound(season, round);
        };
    RecordingMatchRepository repository = new RecordingMatchRepository();
    SeasonMatchImporter importer = new SeasonMatchImporter(provider, repository);

    assertThatThrownBy(() -> importer.importSeason(2026)).isSameAs(failure);
    assertThat(requestedRounds).containsExactly(1, 2, 3);
    assertThat(repository.savedMatches()).isEmpty();
  }

  @Test
  void rejectsMatchesFromAnotherSeasonWithoutSavingAnything() {
    RecordingMatchDataProvider provider =
        new RecordingMatchDataProvider(
            (season, round) ->
                round == 3
                    ? withFirstMatch(validRound(season, round), match(season + 1, round, 1))
                    : validRound(season, round));
    RecordingMatchRepository repository = new RecordingMatchRepository();
    SeasonMatchImporter importer = new SeasonMatchImporter(provider, repository);

    assertThatThrownBy(() -> importer.importSeason(2026))
        .isInstanceOf(InvalidSeasonMatchDataException.class)
        .hasMessage(
            "Provider returned season 2027 for requested season 2026 in round 3 (event event-3-1)");
    assertThat(provider.requestedRounds()).containsExactly(1, 2, 3);
    assertThat(repository.savedMatches()).isEmpty();
  }

  @Test
  void rejectsMatchesFromAnotherRoundWithoutSavingAnything() {
    RecordingMatchDataProvider provider =
        new RecordingMatchDataProvider(
            (season, round) ->
                round == 3
                    ? withFirstMatch(validRound(season, round), match(season, round + 1, 1))
                    : validRound(season, round));
    RecordingMatchRepository repository = new RecordingMatchRepository();
    SeasonMatchImporter importer = new SeasonMatchImporter(provider, repository);

    assertThatThrownBy(() -> importer.importSeason(2026))
        .isInstanceOf(InvalidSeasonMatchDataException.class)
        .hasMessage("Provider returned round 4 for requested round 3 (event event-4-1)");
    assertThat(provider.requestedRounds()).containsExactly(1, 2, 3);
    assertThat(repository.savedMatches()).isEmpty();
  }

  @Test
  void rejectsDuplicateExternalMatchIdsWithoutSavingAnything() {
    RecordingMatchDataProvider provider =
        new RecordingMatchDataProvider(
            (season, round) ->
                round == 2
                    ? withFirstMatch(
                        validRound(season, round), match("event-1-1", season, round, 1))
                    : validRound(season, round));
    RecordingMatchRepository repository = new RecordingMatchRepository();
    SeasonMatchImporter importer = new SeasonMatchImporter(provider, repository);

    assertThatThrownBy(() -> importer.importSeason(2026))
        .isInstanceOf(InvalidSeasonMatchDataException.class)
        .hasMessage("Provider returned duplicate externalMatchId event-1-1 for season 2026");
    assertThat(provider.requestedRounds()).containsExactly(1, 2);
    assertThat(repository.savedMatches()).isEmpty();
  }

  @Test
  void importsAvailableMatchesWhenTheSeasonScheduleIsIncomplete() {
    RecordingMatchDataProvider provider =
        new RecordingMatchDataProvider(
            (season, round) ->
                switch (round) {
                  case 1 -> validRound(season, round);
                  case 2 -> validRound(season, round).subList(0, 4);
                  default -> List.of();
                });
    RecordingMatchRepository repository = new RecordingMatchRepository();
    SeasonMatchImporter importer = new SeasonMatchImporter(provider, repository);

    SeasonMatchImportResult result = importer.importSeason(2026);

    assertThat(result).isEqualTo(new SeasonMatchImportResult(2026, 12));
    assertThat(provider.requestedRounds()).containsExactlyElementsOf(roundsOneThroughThirty());
    assertThat(repository.savedMatches()).hasSize(12);
  }

  @Test
  void rejectsMoreThanEightMatchesInARoundWithoutSavingAnything() {
    RecordingMatchDataProvider provider =
        new RecordingMatchDataProvider(
            (season, round) ->
                round == 3
                    ? IntStream.rangeClosed(1, 9)
                        .mapToObj(matchNumber -> match(season, round, matchNumber))
                        .toList()
                    : validRound(season, round));
    RecordingMatchRepository repository = new RecordingMatchRepository();
    SeasonMatchImporter importer = new SeasonMatchImporter(provider, repository);

    assertThatThrownBy(() -> importer.importSeason(2026))
        .isInstanceOf(InvalidSeasonMatchDataException.class)
        .hasMessage("Provider returned 9 matches for season 2026 round 3; maximum is 8");
    assertThat(provider.requestedRounds()).containsExactly(1, 2, 3);
    assertThat(repository.savedMatches()).isEmpty();
  }

  private List<Integer> roundsOneThroughThirty() {
    return IntStream.rangeClosed(1, 30).boxed().toList();
  }

  private static List<ProviderMatch> validRound(int season, int round) {
    return IntStream.rangeClosed(1, 8)
        .mapToObj(matchNumber -> match(season, round, matchNumber))
        .toList();
  }

  private static List<ProviderMatch> withFirstMatch(
      List<ProviderMatch> matches, ProviderMatch firstMatch) {
    List<ProviderMatch> modifiedMatches = new ArrayList<>(matches);
    modifiedMatches.set(0, firstMatch);
    return List.copyOf(modifiedMatches);
  }

  private static ProviderMatch match(int season, int round, int matchNumber) {
    return match("event-" + round + "-" + matchNumber, season, round, matchNumber);
  }

  private static ProviderMatch match(
      String externalMatchId, int season, int round, int matchNumber) {
    return new ProviderMatch(
        externalMatchId,
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

  private static final class RecordingMatchDataProvider implements MatchDataProvider {

    private final List<Integer> requestedRounds = new ArrayList<>();
    private final RoundResponse response;

    private RecordingMatchDataProvider() {
      this(SeasonMatchImporterTest::validRound);
    }

    private RecordingMatchDataProvider(RoundResponse response) {
      this.response = response;
    }

    @Override
    public List<ProviderMatch> fetchRound(int season, int round) {
      requestedRounds.add(round);
      return response.apply(season, round);
    }

    private List<Integer> requestedRounds() {
      return List.copyOf(requestedRounds);
    }
  }

  @FunctionalInterface
  private interface RoundResponse {

    List<ProviderMatch> apply(int season, int round);
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

package se.matchday.backend.match.application;

import java.util.ArrayList;
import java.util.List;

public final class SeasonMatchImporter {

  private static final int FIRST_ROUND = 1;
  private static final int LAST_ROUND = 30;

  private final MatchDataProvider matchDataProvider;
  private final MatchRepository matchRepository;

  public SeasonMatchImporter(MatchDataProvider matchDataProvider, MatchRepository matchRepository) {
    this.matchDataProvider = matchDataProvider;
    this.matchRepository = matchRepository;
  }

  public SeasonMatchImportResult importSeason(int season) {
    if (season < 1) {
      throw new IllegalArgumentException("season must be a positive integer");
    }

    List<ProviderMatch> matches = new ArrayList<>();
    for (int round = FIRST_ROUND; round <= LAST_ROUND; round++) {
      matches.addAll(matchDataProvider.fetchRound(season, round));
    }
    List<ProviderMatch> importedMatches = List.copyOf(matches);
    matchRepository.saveAll(importedMatches);
    return new SeasonMatchImportResult(season, importedMatches.size());
  }
}

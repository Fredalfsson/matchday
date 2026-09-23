package se.matchday.backend.match.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SeasonMatchImporter {

  private static final int FIRST_ROUND = 1;
  private static final int LAST_ROUND = 30;
  private static final int MAX_MATCHES_PER_ROUND = 8;

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
    Set<String> externalMatchIds = new HashSet<>();
    for (int round = FIRST_ROUND; round <= LAST_ROUND; round++) {
      List<ProviderMatch> roundMatches = matchDataProvider.fetchRound(season, round);
      validateRound(season, round, roundMatches, externalMatchIds);
      matches.addAll(roundMatches);
    }

    List<ProviderMatch> importedMatches = List.copyOf(matches);
    matchRepository.saveAll(importedMatches);
    return new SeasonMatchImportResult(season, importedMatches.size());
  }

  private void validateRound(
      int requestedSeason,
      int requestedRound,
      List<ProviderMatch> roundMatches,
      Set<String> externalMatchIds) {
    if (roundMatches.size() > MAX_MATCHES_PER_ROUND) {
      throw new InvalidSeasonMatchDataException(
          "Provider returned "
              + roundMatches.size()
              + " matches for season "
              + requestedSeason
              + " round "
              + requestedRound
              + "; maximum is "
              + MAX_MATCHES_PER_ROUND);
    }

    for (ProviderMatch match : roundMatches) {
      if (match.season() != requestedSeason) {
        throw new InvalidSeasonMatchDataException(
            "Provider returned season "
                + match.season()
                + " for requested season "
                + requestedSeason
                + " in round "
                + requestedRound
                + " (event "
                + match.externalMatchId()
                + ")");
      }
      if (match.round() != requestedRound) {
        throw new InvalidSeasonMatchDataException(
            "Provider returned round "
                + match.round()
                + " for requested round "
                + requestedRound
                + " (event "
                + match.externalMatchId()
                + ")");
      }
      if (!externalMatchIds.add(match.externalMatchId())) {
        throw new InvalidSeasonMatchDataException(
            "Provider returned duplicate externalMatchId "
                + match.externalMatchId()
                + " for season "
                + requestedSeason);
      }
    }
  }
}

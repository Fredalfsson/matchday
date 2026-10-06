package se.matchday.backend.match.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import se.matchday.backend.match.domain.Match;

public final class MatchQueryService {

  private static final Comparator<Match> DEFAULT_ORDER =
      Comparator.comparingInt((Match match) -> match.season())
          .thenComparingInt(match -> match.round())
          .thenComparing(match -> match.scheduledDate())
          .thenComparing(MatchQueryService::compareKickoffTimes)
          .thenComparing(match -> match.id());

  private final MatchRepository matchRepository;

  public MatchQueryService(MatchRepository matchRepository) {
    this.matchRepository = matchRepository;
  }

  public List<MatchSummary> listMatches() {
    return matchRepository.findAll().stream()
        .sorted(DEFAULT_ORDER)
        .map(MatchQueryService::toSummary)
        .toList();
  }

  private static MatchSummary toSummary(Match match) {
    return new MatchSummary(
        match.id(),
        match.season(),
        match.round(),
        match.homeTeamName(),
        match.awayTeamName(),
        match.scheduledDate(),
        match.kickoffAt(),
        match.status().name(),
        match.homeScore(),
        match.awayScore(),
        match.venueName());
  }

  private static int compareKickoffTimes(Match left, Match right) {
    Instant leftKickoff = left.kickoffAt();
    Instant rightKickoff = right.kickoffAt();

    if (leftKickoff == null) {
      return rightKickoff == null ? 0 : 1;
    }
    if (rightKickoff == null) {
      return -1;
    }
    return leftKickoff.compareTo(rightKickoff);
  }
}

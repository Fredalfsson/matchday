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

  public List<Match> listMatches() {
    return matchRepository.findAll().stream().sorted(DEFAULT_ORDER).toList();
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

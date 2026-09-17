package se.matchday.backend.match.application;

import java.util.List;
import se.matchday.backend.match.domain.Match;

public final class MatchQueryService {

  private final MatchRepository matchRepository;

  public MatchQueryService(MatchRepository matchRepository) {
    this.matchRepository = matchRepository;
  }

  public List<Match> listMatches() {
    return matchRepository.findAll();
  }
}

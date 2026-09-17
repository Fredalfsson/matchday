package se.matchday.backend.match.application;

import java.util.List;
import se.matchday.backend.match.domain.Match;

public interface MatchDataProvider {

  List<Match> fetchRound(int season, int round);
}

package se.matchday.backend.match.application;

import java.util.List;
import se.matchday.backend.match.domain.Match;

public interface MatchRepository {

  void saveAll(List<Match> matches);

  List<Match> findAll();
}

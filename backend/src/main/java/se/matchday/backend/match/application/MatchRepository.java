package se.matchday.backend.match.application;

import java.util.List;
import java.util.UUID;
import se.matchday.backend.match.domain.Match;

public interface MatchRepository {

  void saveAll(List<ProviderMatch> matches);

  List<Match> findAll();

  boolean existsById(UUID matchId);
}

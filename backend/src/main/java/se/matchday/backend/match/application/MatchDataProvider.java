package se.matchday.backend.match.application;

import java.util.List;

public interface MatchDataProvider {

  List<ProviderMatch> fetchRound(int season, int round);
}

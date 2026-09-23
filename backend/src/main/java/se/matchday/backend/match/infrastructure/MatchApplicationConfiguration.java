package se.matchday.backend.match.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.matchday.backend.match.application.MatchDataProvider;
import se.matchday.backend.match.application.MatchQueryService;
import se.matchday.backend.match.application.MatchRepository;
import se.matchday.backend.match.application.SeasonMatchImporter;

@Configuration(proxyBeanMethods = false)
class MatchApplicationConfiguration {

  @Bean
  SeasonMatchImporter seasonMatchImporter(
      MatchDataProvider matchDataProvider, MatchRepository matchRepository) {
    return new SeasonMatchImporter(matchDataProvider, matchRepository);
  }

  @Bean
  MatchQueryService matchQueryService(MatchRepository matchRepository) {
    return new MatchQueryService(matchRepository);
  }
}

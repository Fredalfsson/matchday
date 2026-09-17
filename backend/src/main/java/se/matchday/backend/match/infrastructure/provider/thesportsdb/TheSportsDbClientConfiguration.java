package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;
import se.matchday.backend.match.application.MatchDataProvider;
import se.matchday.backend.match.application.MatchRepository;
import se.matchday.backend.match.application.SeasonMatchImporter;

@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "the-sports-db", types = TheSportsDbClient.class)
@EnableConfigurationProperties(TheSportsDbProperties.class)
class TheSportsDbClientConfiguration {

  @Bean
  TheSportsDbRequestExecutor theSportsDbRequestExecutor(TheSportsDbProperties properties) {
    return new TheSportsDbRequestExecutor(properties, Clock.systemUTC(), Thread::sleep);
  }

  @Bean
  MatchDataProvider matchDataProvider(
      TheSportsDbClient client,
      TheSportsDbProperties properties,
      TheSportsDbRequestExecutor requestExecutor) {
    return new TheSportsDbMatchDataProvider(
        client, new TheSportsDbEventMapper(), properties, requestExecutor);
  }

  @Bean
  SeasonMatchImporter seasonMatchImporter(
      MatchDataProvider matchDataProvider, MatchRepository matchRepository) {
    return new SeasonMatchImporter(matchDataProvider, matchRepository);
  }
}

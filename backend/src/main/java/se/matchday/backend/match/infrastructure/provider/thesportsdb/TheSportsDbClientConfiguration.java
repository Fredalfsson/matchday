package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;
import se.matchday.backend.match.application.MatchDataProvider;

@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "the-sports-db", types = TheSportsDbClient.class)
@EnableConfigurationProperties(TheSportsDbProperties.class)
class TheSportsDbClientConfiguration {

  @Bean
  MatchDataProvider matchDataProvider(TheSportsDbClient client, TheSportsDbProperties properties) {
    return new TheSportsDbMatchDataProvider(client, new TheSportsDbEventMapper(), properties);
  }
}

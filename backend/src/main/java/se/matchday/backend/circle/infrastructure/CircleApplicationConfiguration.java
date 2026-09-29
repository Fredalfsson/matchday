package se.matchday.backend.circle.infrastructure;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.matchday.backend.circle.application.CircleCreationService;
import se.matchday.backend.circle.application.CircleMembershipService;
import se.matchday.backend.circle.application.CircleQueryService;
import se.matchday.backend.circle.application.CircleRepository;
import se.matchday.backend.identity.application.CurrentUser;
import se.matchday.backend.match.application.MatchRepository;

@Configuration(proxyBeanMethods = false)
class CircleApplicationConfiguration {

  @Bean
  CircleCreationService circleCreationService(
      CurrentUser currentUser, MatchRepository matchRepository, CircleRepository circleRepository) {
    return new CircleCreationService(
        currentUser, matchRepository, circleRepository, Clock.systemUTC());
  }

  @Bean
  CircleQueryService circleQueryService(
      CurrentUser currentUser, MatchRepository matchRepository, CircleRepository circleRepository) {
    return new CircleQueryService(currentUser, matchRepository, circleRepository);
  }

  @Bean
  CircleMembershipService circleMembershipService(
      CurrentUser currentUser, MatchRepository matchRepository, CircleRepository circleRepository) {
    return new CircleMembershipService(
        currentUser, matchRepository, circleRepository, Clock.systemUTC());
  }
}

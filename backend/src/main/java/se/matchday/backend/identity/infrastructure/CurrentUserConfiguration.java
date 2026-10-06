package se.matchday.backend.identity.infrastructure;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Fallback;
import se.matchday.backend.identity.application.CurrentUser;

@Configuration(proxyBeanMethods = false)
class CurrentUserConfiguration {

  @Bean
  @Fallback
  CurrentUser unavailableCurrentUser() {
    return Optional::empty;
  }
}

package se.matchday.backend.identity.infrastructure;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Fallback;
import se.matchday.backend.identity.application.UserDirectory;
import se.matchday.backend.identity.application.UserDirectoryUnavailableException;

@Configuration(proxyBeanMethods = false)
class UserDirectoryConfiguration {

  @Bean
  @Fallback
  UserDirectory unavailableUserDirectory() {
    return userIds -> {
      if (userIds.isEmpty()) {
        return Map.of();
      }
      throw new UserDirectoryUnavailableException();
    };
  }
}

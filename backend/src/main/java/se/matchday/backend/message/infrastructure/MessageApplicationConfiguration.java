package se.matchday.backend.message.infrastructure;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.matchday.backend.circle.application.CircleRepository;
import se.matchday.backend.identity.application.CurrentUser;
import se.matchday.backend.identity.application.UserDirectory;
import se.matchday.backend.message.application.MessageCreationService;
import se.matchday.backend.message.application.MessageRepository;

@Configuration(proxyBeanMethods = false)
class MessageApplicationConfiguration {

  @Bean
  MessageCreationService messageCreationService(
      CurrentUser currentUser,
      UserDirectory userDirectory,
      CircleRepository circleRepository,
      MessageRepository messageRepository) {
    return new MessageCreationService(
        currentUser, userDirectory, circleRepository, messageRepository, Clock.systemUTC());
  }
}

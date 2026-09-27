package se.matchday.backend.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import se.matchday.backend.identity.application.CurrentUser;

class CurrentUserConfigurationTest {

  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(CurrentUserConfiguration.class);

  @Test
  void deniesAccessWhenNoAuthenticationAdapterIsAvailable() {
    contextRunner.run(context -> assertThat(context.getBean(CurrentUser.class).userId()).isEmpty());
  }

  @Test
  void authenticationAdapterTakesPrecedenceOverTheFallback() {
    CurrentUser authenticatedUser = () -> Optional.of(USER_ID);

    contextRunner
        .withBean(CurrentUser.class, () -> authenticatedUser)
        .run(
            context -> {
              assertThat(context).hasSingleBean(CurrentUserConfiguration.class);
              assertThat(context.getBeansOfType(CurrentUser.class)).hasSize(2);
              assertThat(context.getBean(CurrentUser.class)).isSameAs(authenticatedUser);
            });
  }
}

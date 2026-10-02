package se.matchday.backend.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import se.matchday.backend.identity.application.UserDirectory;
import se.matchday.backend.identity.application.UserDirectoryUnavailableException;

class UserDirectoryConfigurationTest {

  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(UserDirectoryConfiguration.class);

  @Test
  void returnsAnEmptyResultWithoutRequiringAUserDirectoryAdapter() {
    contextRunner.run(
        context ->
            assertThat(context.getBean(UserDirectory.class).findUsernamesByUserIds(Set.of()))
                .isEmpty());
  }

  @Test
  void failsClosedWhenPublicUserDataIsRequiredWithoutAnAdapter() {
    contextRunner.run(
        context ->
            assertThatThrownBy(
                    () ->
                        context
                            .getBean(UserDirectory.class)
                            .findUsernamesByUserIds(Set.of(USER_ID)))
                .isInstanceOf(UserDirectoryUnavailableException.class)
                .hasMessage("The public user directory is unavailable"));
  }

  @Test
  void userDirectoryAdapterTakesPrecedenceOverTheFallback() {
    UserDirectory userDirectory = userIds -> Map.of(USER_ID, "sara");

    contextRunner
        .withBean(UserDirectory.class, () -> userDirectory)
        .run(
            context -> {
              assertThat(context).hasSingleBean(UserDirectoryConfiguration.class);
              assertThat(context.getBeansOfType(UserDirectory.class)).hasSize(2);
              assertThat(context.getBean(UserDirectory.class)).isSameAs(userDirectory);
              assertThat(
                      context.getBean(UserDirectory.class).findUsernamesByUserIds(Set.of(USER_ID)))
                  .containsExactlyEntriesOf(Map.of(USER_ID, "sara"));
            });
  }
}

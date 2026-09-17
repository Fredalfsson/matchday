package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param apiKey API key used in TheSportsDB v1 request paths
 * @param leagueId TheSportsDB league identifier for Allsvenskan
 */
@Validated
@ConfigurationProperties("matchday.providers.the-sports-db")
record TheSportsDbProperties(@NotBlank String apiKey, @NotBlank String leagueId) {

  @Override
  public String toString() {
    return "TheSportsDbProperties[apiKey=***, leagueId=" + leagueId + "]";
  }
}

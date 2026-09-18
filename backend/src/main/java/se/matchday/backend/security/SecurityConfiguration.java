package se.matchday.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration(proxyBeanMethods = false)
@Profile("!local")
class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return configureApiAccess(http).build();
  }

  static HttpSecurity configureApiAccess(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("MATCH_IMPORTER")
                    .requestMatchers(HttpMethod.GET, "/api/v1/matches")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
  }
}

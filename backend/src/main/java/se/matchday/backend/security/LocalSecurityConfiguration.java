package se.matchday.backend.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@Profile("local")
@EnableConfigurationProperties(LocalSecurityProperties.class)
class LocalSecurityConfiguration {

  @Bean
  SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
    return SecurityConfiguration.configureApiAccess(http)
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(Customizer.withDefaults())
        .build();
  }

  @Bean
  PasswordEncoder localPasswordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  UserDetailsService localUserDetailsService(
      LocalSecurityProperties properties, PasswordEncoder passwordEncoder) {
    UserDetails localOperator =
        User.builder()
            .username(properties.username())
            .password(passwordEncoder.encode(properties.password()))
            .roles("MATCH_IMPORTER")
            .build();

    return new InMemoryUserDetailsManager(localOperator);
  }
}

package se.matchday.backend.match.infrastructure.provider.thesportsdb;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "the-sports-db", types = TheSportsDbClient.class)
class TheSportsDbClientConfiguration {}

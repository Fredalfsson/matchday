package se.matchday.backend.match.infrastructure.persistence.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.matchday.backend.match.application.MatchRepository;
import se.matchday.backend.match.application.ProviderMatch;
import se.matchday.backend.match.domain.Match;

@Repository
class JpaMatchRepositoryAdapter implements MatchRepository {

  private final SpringDataMatchJpaRepository repository;

  JpaMatchRepositoryAdapter(SpringDataMatchJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void saveAll(List<ProviderMatch> matches) {
    Objects.requireNonNull(matches, "matches must not be null");
    if (matches.isEmpty()) {
      return;
    }

    Set<String> externalMatchIds = new HashSet<>();
    for (ProviderMatch match : matches) {
      externalMatchIds.add(
          Objects.requireNonNull(match, "matches must not contain null").externalMatchId());
    }

    Map<String, MatchJpaEntity> entitiesByExternalMatchId = new HashMap<>();
    for (MatchJpaEntity entity : repository.findAllByExternalMatchIdIn(externalMatchIds)) {
      entitiesByExternalMatchId.put(entity.externalMatchId(), entity);
    }

    List<MatchJpaEntity> newEntities = new ArrayList<>();
    for (ProviderMatch match : matches) {
      MatchJpaEntity entity = entitiesByExternalMatchId.get(match.externalMatchId());
      if (entity == null) {
        entity = new MatchJpaEntity(match);
        entitiesByExternalMatchId.put(match.externalMatchId(), entity);
        newEntities.add(entity);
      } else {
        entity.updateFrom(match);
      }
    }
    repository.saveAll(newEntities);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Match> findAll() {
    return repository.findAll().stream().map(entity -> entity.toDomain()).toList();
  }
}

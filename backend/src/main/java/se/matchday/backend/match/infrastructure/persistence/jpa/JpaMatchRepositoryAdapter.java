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
import se.matchday.backend.match.domain.Match;

@Repository
class JpaMatchRepositoryAdapter implements MatchRepository {

  private final SpringDataMatchJpaRepository repository;

  JpaMatchRepositoryAdapter(SpringDataMatchJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void saveAll(List<Match> matches) {
    Objects.requireNonNull(matches, "matches must not be null");
    if (matches.isEmpty()) {
      return;
    }

    Set<String> externalIds = new HashSet<>();
    for (Match match : matches) {
      externalIds.add(Objects.requireNonNull(match, "matches must not contain null").externalId());
    }

    Map<String, MatchJpaEntity> entitiesByExternalId = new HashMap<>();
    for (MatchJpaEntity entity : repository.findAllByExternalIdIn(externalIds)) {
      entitiesByExternalId.put(entity.externalId(), entity);
    }

    List<MatchJpaEntity> newEntities = new ArrayList<>();
    for (Match match : matches) {
      MatchJpaEntity entity = entitiesByExternalId.get(match.externalId());
      if (entity == null) {
        entity = new MatchJpaEntity(match);
        entitiesByExternalId.put(match.externalId(), entity);
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

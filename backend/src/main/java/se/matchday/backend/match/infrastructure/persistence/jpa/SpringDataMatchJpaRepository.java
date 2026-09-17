package se.matchday.backend.match.infrastructure.persistence.jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMatchJpaRepository extends JpaRepository<MatchJpaEntity, UUID> {

  List<MatchJpaEntity> findAllByExternalIdIn(Collection<String> externalIds);
}

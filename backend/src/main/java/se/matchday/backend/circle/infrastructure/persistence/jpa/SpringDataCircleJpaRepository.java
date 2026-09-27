package se.matchday.backend.circle.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCircleJpaRepository extends JpaRepository<CircleJpaEntity, UUID> {}

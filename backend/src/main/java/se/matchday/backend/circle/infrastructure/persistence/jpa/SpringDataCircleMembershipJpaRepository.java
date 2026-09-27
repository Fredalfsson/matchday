package se.matchday.backend.circle.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCircleMembershipJpaRepository
    extends JpaRepository<CircleMembershipJpaEntity, CircleMembershipJpaId> {}

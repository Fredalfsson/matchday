package se.matchday.backend.circle.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

interface SpringDataCircleMembershipJpaRepository
    extends JpaRepository<CircleMembershipJpaEntity, CircleMembershipJpaId> {

  boolean existsByCircleIdAndUserId(UUID circleId, UUID userId);

  @Modifying
  @NativeQuery(
      """
      INSERT INTO circle_memberships (circle_id, user_id, joined_at)
      VALUES (:circleId, :userId, :joinedAt)
      ON CONFLICT (circle_id, user_id) DO NOTHING
      """)
  int insertIfAbsent(
      @Param("circleId") UUID circleId,
      @Param("userId") UUID userId,
      @Param("joinedAt") Instant joinedAt);

  @Modifying
  @NativeQuery(
      """
      DELETE FROM circle_memberships
      WHERE circle_id = :circleId AND user_id = :userId
      """)
  int deleteIfPresent(@Param("circleId") UUID circleId, @Param("userId") UUID userId);
}

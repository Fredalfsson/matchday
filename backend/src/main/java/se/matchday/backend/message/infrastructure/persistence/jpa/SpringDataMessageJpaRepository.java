package se.matchday.backend.message.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

interface SpringDataMessageJpaRepository extends JpaRepository<MessageJpaEntity, UUID> {

  @Modifying
  @NativeQuery(
      """
      INSERT INTO messages (id, circle_id, author_user_id, content, created_at)
      SELECT :messageId, membership.circle_id, membership.user_id, :content, :createdAt
      FROM circle_memberships AS membership
      WHERE membership.circle_id = :circleId
        AND membership.user_id = :authorUserId
      FOR KEY SHARE OF membership
      """)
  int insertIfActiveMember(
      @Param("messageId") UUID messageId,
      @Param("circleId") UUID circleId,
      @Param("authorUserId") UUID authorUserId,
      @Param("content") String content,
      @Param("createdAt") Instant createdAt);
}

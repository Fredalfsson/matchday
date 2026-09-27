package se.matchday.backend.circle.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.matchday.backend.circle.application.CircleAlreadyExistsException;
import se.matchday.backend.circle.application.CircleRepository;
import se.matchday.backend.circle.domain.Circle;

@Repository
class JpaCircleRepositoryAdapter implements CircleRepository {

  private static final String MATCH_ID_UNIQUE_CONSTRAINT = "circles_match_id_unique";

  private final SpringDataCircleJpaRepository circleRepository;
  private final SpringDataCircleMembershipJpaRepository membershipRepository;

  JpaCircleRepositoryAdapter(
      SpringDataCircleJpaRepository circleRepository,
      SpringDataCircleMembershipJpaRepository membershipRepository) {
    this.circleRepository = circleRepository;
    this.membershipRepository = membershipRepository;
  }

  @Override
  @Transactional
  public Circle createWithCreatorMembership(UUID matchId, UUID creatorUserId, Instant createdAt) {
    Objects.requireNonNull(matchId, "matchId must not be null");
    Objects.requireNonNull(creatorUserId, "creatorUserId must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");

    CircleJpaEntity circle = new CircleJpaEntity(matchId, creatorUserId, createdAt);
    try {
      circleRepository.saveAndFlush(circle);
    } catch (DataIntegrityViolationException exception) {
      if (causedByConstraint(exception, MATCH_ID_UNIQUE_CONSTRAINT)) {
        throw new CircleAlreadyExistsException(matchId, exception);
      }
      throw exception;
    }

    membershipRepository.saveAndFlush(
        new CircleMembershipJpaEntity(circle.id(), creatorUserId, createdAt));
    return circle.toDomain();
  }

  private static boolean causedByConstraint(Throwable failure, String constraintName) {
    Throwable cause = failure;
    while (cause != null) {
      if (cause instanceof ConstraintViolationException violation
          && constraintName.equals(violation.getConstraintName())) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }
}

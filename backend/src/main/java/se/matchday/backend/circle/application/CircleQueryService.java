package se.matchday.backend.circle.application;

import java.util.Objects;
import java.util.UUID;
import se.matchday.backend.circle.domain.Circle;
import se.matchday.backend.identity.application.CurrentUser;
import se.matchday.backend.identity.application.CurrentUserUnavailableException;
import se.matchday.backend.match.application.MatchRepository;

public final class CircleQueryService {

  private final CurrentUser currentUser;
  private final MatchRepository matchRepository;
  private final CircleRepository circleRepository;

  public CircleQueryService(
      CurrentUser currentUser, MatchRepository matchRepository, CircleRepository circleRepository) {
    this.currentUser = currentUser;
    this.matchRepository = matchRepository;
    this.circleRepository = circleRepository;
  }

  public CircleMembershipStatus findForMatch(UUID matchId) {
    Objects.requireNonNull(matchId, "matchId must not be null");
    UUID userId = currentUser.userId().orElseThrow(CurrentUserUnavailableException::new);
    if (!matchRepository.existsById(matchId)) {
      throw new MatchNotFoundException(matchId);
    }

    Circle circle =
        circleRepository
            .findByMatchId(matchId)
            .orElseThrow(() -> new CircleNotFoundForMatchException(matchId));
    boolean membershipActive = circleRepository.hasActiveMembership(circle.id(), userId);
    return new CircleMembershipStatus(
        circle.id(), circle.matchId(), circle.createdAt(), membershipActive);
  }
}

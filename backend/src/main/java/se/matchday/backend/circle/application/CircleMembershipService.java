package se.matchday.backend.circle.application;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import se.matchday.backend.circle.domain.Circle;
import se.matchday.backend.identity.application.CurrentUser;
import se.matchday.backend.match.application.MatchRepository;

public final class CircleMembershipService {

  private final CurrentUser currentUser;
  private final MatchRepository matchRepository;
  private final CircleRepository circleRepository;
  private final Clock clock;

  public CircleMembershipService(
      CurrentUser currentUser,
      MatchRepository matchRepository,
      CircleRepository circleRepository,
      Clock clock) {
    this.currentUser = currentUser;
    this.matchRepository = matchRepository;
    this.circleRepository = circleRepository;
    this.clock = clock;
  }

  public void joinCircleForMatch(UUID matchId) {
    Objects.requireNonNull(matchId, "matchId must not be null");
    UUID userId = currentUser.userId().orElseThrow(CurrentUserUnavailableException::new);
    Circle circle = requireCircleForMatch(matchId);
    circleRepository.addMembershipIfAbsent(circle.id(), userId, clock.instant());
  }

  public void leaveCircleForMatch(UUID matchId) {
    Objects.requireNonNull(matchId, "matchId must not be null");
    UUID userId = currentUser.userId().orElseThrow(CurrentUserUnavailableException::new);
    Circle circle = requireCircleForMatch(matchId);
    circleRepository.removeMembershipIfPresent(circle.id(), userId);
  }

  private Circle requireCircleForMatch(UUID matchId) {
    if (!matchRepository.existsById(matchId)) {
      throw new MatchNotFoundException(matchId);
    }

    return circleRepository
        .findByMatchId(matchId)
        .orElseThrow(() -> new CircleNotFoundException(matchId));
  }
}

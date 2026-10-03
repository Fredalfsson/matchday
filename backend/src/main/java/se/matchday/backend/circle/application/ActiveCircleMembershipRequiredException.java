package se.matchday.backend.circle.application;

import java.util.UUID;

public final class ActiveCircleMembershipRequiredException extends RuntimeException {

  public ActiveCircleMembershipRequiredException(UUID circleId) {
    super("An active membership is required for circle " + circleId);
  }
}

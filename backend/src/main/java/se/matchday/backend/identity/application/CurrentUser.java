package se.matchday.backend.identity.application;

import java.util.Optional;
import java.util.UUID;

/** Supplies the authenticated user's stable application identity. */
public interface CurrentUser {

  Optional<UUID> userId();
}

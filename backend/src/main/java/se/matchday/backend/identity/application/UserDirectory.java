package se.matchday.backend.identity.application;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Supplies public usernames for stable application user identities. */
public interface UserDirectory {

  Map<UUID, String> findUsernamesByUserIds(Set<UUID> userIds);
}

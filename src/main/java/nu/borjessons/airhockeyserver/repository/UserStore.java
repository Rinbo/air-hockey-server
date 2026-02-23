package nu.borjessons.airhockeyserver.repository;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nu.borjessons.airhockeyserver.model.Username;

public class UserStore {
  private final ConcurrentHashMap<Username, Username> users;

  public UserStore() {
    this.users = new ConcurrentHashMap<>();
  }

  public boolean addUser(Username username) {
    Objects.requireNonNull(username, "username must not be null");

    return users.putIfAbsent(username, username) == null;
  }

  public boolean contains(Username username) {
    return users.containsKey(username);
  }

  public Set<Username> getAll() {
    return Collections.unmodifiableSet(users.keySet());
  }

  public void heartbeat(Username username) {
    Username stored = users.get(username);
    if (stored != null)
      stored.setInstant(Instant.now());
  }

  public void removeUser(Username username) {
    Objects.requireNonNull(username, "username must not be null");

    users.remove(username);
  }
}

package nu.borjessons.airhockeyserver.repository;

import java.util.Objects;
import java.util.Set;

import nu.borjessons.airhockeyserver.model.Username;

public class UserStore {
  private final Set<Username> users;

  public UserStore(Set<Username> users) {
    Objects.requireNonNull(users, "users must not be null");

    this.users = users;
  }

  public boolean addUser(Username username) {
    Objects.requireNonNull(username, "username must not be null");

    return users.add(username);
  }

  public boolean contains(Username username) {
    return users.contains(username);
  }

  public void removeUser(Username username) {
    Objects.requireNonNull(username, "username must not be null");

    users.remove(username);
  }
}

package nu.borjessons.airhockeyserver.model;

import java.util.Objects;

public record Player(Username username, Agent agent) {
  public Player {
    Objects.requireNonNull(username, "username must not be null");
    Objects.requireNonNull(agent, "agent must not be null");
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;

    Player other = (Player) object;

    return username.equals(other.username);
  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }
}

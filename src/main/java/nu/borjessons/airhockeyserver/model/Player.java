package nu.borjessons.airhockeyserver.model;

import java.util.Objects;

public record Player(Username username, Agent agent) {
  public Player {
    Objects.requireNonNull(username, "username must not be null");
    Objects.requireNonNull(agent, "agent must not be null");
  }
}

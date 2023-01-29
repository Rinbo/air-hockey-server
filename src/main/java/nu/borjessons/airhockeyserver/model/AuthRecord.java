package nu.borjessons.airhockeyserver.model;

import java.util.Objects;

public record AuthRecord(GameId gameId, Username username) {
  public AuthRecord {
    Objects.requireNonNull(gameId, "gameId must not be null");
    Objects.requireNonNull(username, "username must not be null");
  }
}

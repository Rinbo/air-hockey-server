package nu.borjessons.airhockeyserver.model;

import java.util.Objects;

import nu.borjessons.airhockeyserver.repository.GameStore;

public record Game(GameId gameId, Username username, boolean joinable) {
  public Game {
    Objects.requireNonNull(gameId, "gameId must not be null");
    Objects.requireNonNull(username, "username must not be null");
  }

  public Game(GameStore gameStore) {
    this(gameStore.getGameId(), gameStore.getGameCreator().orElseThrow(), gameStore.isJoinable());
  }
}

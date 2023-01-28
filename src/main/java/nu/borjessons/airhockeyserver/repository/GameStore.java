package nu.borjessons.airhockeyserver.repository;

import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.GameState;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;

public class GameStore {
  private final GameId gameId;
  private GameState gameState;
  private final Set<Player> players;

  public GameStore(GameId gameId) {
    this.gameId = gameId;
    this.gameState = GameState.LOBBY;
    this.players = new TreeSet<>();
  }

  public synchronized void addPlayer(Player player) {
    if (players.size() < 2) {
      players.add(player);
    }
  }

  public GameId getGameId() {
    return gameId;
  }

  public synchronized GameState getGameState() {
    return gameState;
  }

  public synchronized Optional<Player> getPlayer(Username username) {
    return players.stream().filter(player -> player.isPlayer(username)).findFirst();
  }

  public synchronized Set<Player> getPlayers() {
    return players;
  }

  public synchronized void removePlayer(Player player) {
    players.remove(player);
  }

  public synchronized void setGameState(GameState gameState) {
    this.gameState = gameState;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", GameStore.class.getSimpleName() + "[", "]")
        .add("gameId=" + gameId)
        .add("players=" + players)
        .add("gameState=" + gameState)
        .toString();
  }

  public synchronized void togglePlayerReadiness(Player player) {
    players.stream().filter(player::equals).findFirst().ifPresent(Player::toggleReady);
  }
}

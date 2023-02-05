package nu.borjessons.airhockeyserver.repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.engine.GameEngine;
import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.GameState;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;

public class GameStore {
  private final GameEngine gameEngine;
  private final GameId gameId;
  private GameState gameState; // Change to using atomic reference
  private final Set<Player> players;

  public GameStore(GameId gameId) {
    this.gameId = gameId;
    this.gameState = GameState.LOBBY;
    this.players = new HashSet<>();
    this.gameEngine = GameEngine.create();
  }

  public synchronized boolean addPlayer(Username username) {
    if (players.size() > 1)
      return false;

    return switch (players.size()) {
      case 0 -> players.add(new Player(Agency.PLAYER_1, username));
      case 1 -> players.add(new Player(Agency.PLAYER_2, username));
      default -> false;
    };
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

  public synchronized Collection<Player> getPlayers() {
    return players.stream().sorted(Comparator.comparing(Player::getAgency)).toList();
  }

  public synchronized void removePlayer(Player player) {
    players.remove(player);
  }

  public synchronized void setGameState(GameState gameState) {
    this.gameState = gameState;
  }

  public void startGame(SimpMessagingTemplate messagingTemplate) {
    setGameState(GameState.GAME_RUNNING);
    gameEngine.startGame(gameId, messagingTemplate);
  }

  public void terminate() {
    gameEngine.terminate();
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

package nu.borjessons.airhockeyserver.repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.game.BoardState;
import nu.borjessons.airhockeyserver.game.GameEngine;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.GameState;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;

import nu.borjessons.airhockeyserver.websocket.GameWebSocketHandler;

public class GameStore {
  private static final Logger logger = LoggerFactory.getLogger(GameStore.class);

  private static final Username AI_USERNAME = new Username("AI Bot");

  private final GameEngine gameEngine;
  private final GameId gameId;
  private final AtomicReference<GameState> gameStateReference;
  private final Set<Player> players;
  private boolean aiMode;

  public GameStore(GameId gameId) {
    this.gameId = gameId;
    this.gameStateReference = new AtomicReference<>(GameState.LOBBY);
    this.players = new HashSet<>();
    this.gameEngine = GameEngine.create();
  }

  private static GameState validateTransition(GameState newGameState, GameState currentGameState) {
    if (!currentGameState.isValidNextState(newGameState)) {
      logger.warn("illegal transition: {} -> {}", currentGameState, newGameState);
      throw new IllegalStateException("illegal transition");
    }
    return newGameState;
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

  public synchronized void addAiPlayer() {
    if (players.size() != 1) {
      throw new IllegalStateException("Can only add AI when exactly one human player is present");
    }
    Player aiPlayer = new Player(Agency.PLAYER_2, AI_USERNAME);
    players.add(aiPlayer);
    aiPlayer.toggleReady();
    this.aiMode = true;
    gameEngine.setAiMode(true);
  }

  public Optional<Username> getGameCreator() {
    return players.stream().filter(player -> player.getAgency() == Agency.PLAYER_1).map(Player::getUsername)
        .findFirst();
  }

  public GameId getGameId() {
    return gameId;
  }

  public GameState getGameState() {
    return gameStateReference.get();
  }

  public synchronized Optional<Player> getPlayer(Username username) {
    return players.stream().filter(player -> player.isPlayer(username)).findFirst();
  }

  public synchronized Collection<Player> getPlayers() {
    return List.copyOf(players);
  }

  public boolean isAiMode() {
    return aiMode;
  }

  public boolean isJoinable() {
    return !aiMode && players.size() != 2;
  }

  public synchronized void removePlayer(Player player) {
    players.remove(player);
  }

  public void startGame(SimpMessagingTemplate messagingTemplate, GameWebSocketHandler gameWebSocketHandler) {
    transition(GameState.GAME_RUNNING);
    gameEngine.startGame(gameId, new GameStoreConnector(this, messagingTemplate, gameWebSocketHandler));
  }

  public void terminate() {
    gameEngine.terminate();
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", GameStore.class.getSimpleName() + "[", "]")
        .add("gameId=" + gameId)
        .add("players=" + players)
        .add("gameState=" + gameStateReference)
        .toString();
  }

  public synchronized void togglePlayerReadiness(Player player) {
    players.stream().filter(player::equals).findFirst().ifPresent(Player::toggleReady);

    // In AI mode, ensure the bot is always ready
    if (aiMode) {
      players.stream()
          .filter(p -> p.getAgency() == Agency.PLAYER_2 && !p.isReady())
          .forEach(Player::toggleReady);
    }
  }

  public void transition(GameState newGameState) {
    gameStateReference.updateAndGet(currentGameState -> validateTransition(newGameState, currentGameState));
  }

  public void updateHandle(Position position, Agency agency) {
    Objects.requireNonNull(position, "position must not be null");
    Objects.requireNonNull(agency, "agency must not be null");

    if (gameStateReference.get() != GameState.GAME_RUNNING) {
      return;
    }

    switch (agency) {
      case PLAYER_1 -> gameEngine.updateHandle(BoardState::playerOne, position);
      case PLAYER_2 -> gameEngine.updateHandle(BoardState::playerTwo, GameEngine.mirror(position));
    }
  }
}

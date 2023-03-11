package nu.borjessons.airhockeyserver.repository;

import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.game.BroadcastState;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.GameState;

public class GameStoreController {
  private final GameId gameId;
  private final Consumer<GameState> gameStateUpdater;
  private final SimpMessagingTemplate messagingTemplate;
  private final String playerOneTopic;
  private final String playerTwoTopic;
  private final Runnable terminationCallback;

  public GameStoreController(GameId gameId, Consumer<GameState> gameStateUpdater, SimpMessagingTemplate messagingTemplate, Runnable terminationCallback) {
    Objects.requireNonNull(gameId, "gameId must not be null");
    Objects.requireNonNull(gameStateUpdater, "gameStateUpdater must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
    Objects.requireNonNull(terminationCallback, "terminationCallback must not be null");

    this.gameId = gameId;
    this.gameStateUpdater = gameStateUpdater;
    this.messagingTemplate = messagingTemplate;
    this.playerOneTopic = String.format("/topic/game/%s/board-state/player-1", gameId);
    this.playerTwoTopic = String.format("/topic/game/%s/board-state/player-2", gameId);
    this.terminationCallback = terminationCallback;
  }

  public void broadcast(BroadcastState playerOneState, BroadcastState playerTwoState) {
    messagingTemplate.convertAndSend(playerOneTopic, playerOneState);
    messagingTemplate.convertAndSend(playerTwoTopic, playerTwoState);
  }

  public void terminateGame() {
    terminationCallback.run();
  }

  public void updateState(GameState gameState) {
    gameStateUpdater.accept(gameState);
  }
}

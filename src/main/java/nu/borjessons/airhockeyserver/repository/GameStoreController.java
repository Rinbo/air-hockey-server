package nu.borjessons.airhockeyserver.repository;

import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.game.BroadcastState;
import nu.borjessons.airhockeyserver.model.GameState;

public class GameStoreController {
  private final Consumer<GameState> gameStateUpdater;
  private final SimpMessagingTemplate messagingTemplate;
  private final Runnable terminationCallback;

  public GameStoreController(Consumer<GameState> gameStateUpdater, SimpMessagingTemplate messagingTemplate, Runnable terminationCallback) {
    Objects.requireNonNull(gameStateUpdater, "gameStateUpdater must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
    Objects.requireNonNull(terminationCallback, "terminationCallback must not be null");

    this.gameStateUpdater = gameStateUpdater;
    this.messagingTemplate = messagingTemplate;
    this.terminationCallback = terminationCallback;
  }

  public void broadcast(String playerOneTopic, String playerTwoTopic, BroadcastState stateP1, BroadcastState stateP2) {
    messagingTemplate.convertAndSend(playerOneTopic, stateP1);
    messagingTemplate.convertAndSend(playerTwoTopic, stateP2);
  }

  public void terminateGame() {
    terminationCallback.run();
  }

  public void updateState(GameState gameState) {
    gameStateUpdater.accept(gameState);
  }
}

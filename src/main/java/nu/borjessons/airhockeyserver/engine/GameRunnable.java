package nu.borjessons.airhockeyserver.engine;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.messaging.simp.SimpMessagingTemplate;

public class GameRunnable implements Runnable {
  private final AtomicReference<GameState> gameState;
  private final SimpMessagingTemplate messagingTemplate;

  public GameRunnable(AtomicReference<GameState> gameState, SimpMessagingTemplate messagingTemplate) {
    Objects.requireNonNull(gameState, "gameState must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");

    this.gameState = gameState;
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void run() {
    // Run
  }
}

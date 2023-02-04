package nu.borjessons.airhockeyserver.engine;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Start simple. Only render the two player's handles at first
 * TODO: probably need previous tick's state as well so that I can calculate the speed in the next tick
 */
public class GameEngine {
  private final AtomicReference<GameState> gameState;
  private final Thread gameThread;

  private GameEngine(AtomicReference<GameState> gameState, Thread thread) {

    this.gameState = gameState;
    this.gameThread = thread;
  }

  public static GameEngine init(SimpMessagingTemplate messagingTemplate) {
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");

    AtomicReference<GameState> gameState = new AtomicReference<>(GameConstants.createInitialGameState());
    return new GameEngine(gameState, new Thread(new GameRunnable(gameState, messagingTemplate)));
  }

  void startGame() {
    gameThread.start();
  }
}

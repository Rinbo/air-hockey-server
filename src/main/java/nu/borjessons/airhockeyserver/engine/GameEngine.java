package nu.borjessons.airhockeyserver.engine;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.model.GameId;

/**
 * Start simple. Only render the two player's handles at first
 * TODO: probably need previous tick's state as well so that I can calculate the speed in the next tick
 */
public class GameEngine {
  private final AtomicReference<GameState> atomicReference;
  private final Thread gameThread;

  private GameEngine(AtomicReference<GameState> atomicReference, Thread thread) {
    this.atomicReference = atomicReference;
    this.gameThread = thread;
  }

  public static GameEngine init(GameId gameId, SimpMessagingTemplate messagingTemplate) {
    Objects.requireNonNull(gameId, "gameId must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");

    AtomicReference<GameState> atomicReference = new AtomicReference<>(GameConstants.createInitialGameState());
    return new GameEngine(atomicReference, new Thread(new GameRunnable(atomicReference, messagingTemplate, String.format("/topic/game/%s", gameId))));
  }

  void startGame() {
    gameThread.start();
  }
}

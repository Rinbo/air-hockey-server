package nu.borjessons.airhockeyserver.engine;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.model.GameId;

/**
 * Start simple. Only render the two player's handles at first
 * TODO: probably need previous tick's state as well so that I can calculate the speed in the next tick
 */
public class GameEngine {
  private final AtomicReference<BoardState> boardStateReference;
  private final AtomicReference<Thread> gameThreadReference;

  private GameEngine(AtomicReference<BoardState> boardStateReference, AtomicReference<Thread> thread) {
    this.boardStateReference = boardStateReference;
    this.gameThreadReference = thread;
  }

  public static GameEngine create() {
    AtomicReference<BoardState> atomicReference = new AtomicReference<>(GameConstants.createInitialGameState());
    return new GameEngine(atomicReference, new AtomicReference<>());
  }

  public void startGame(GameId gameId, SimpMessagingTemplate messagingTemplate) {
    Thread thread = new Thread(new GameRunnable(boardStateReference, gameId, messagingTemplate));
    thread.start();
    gameThreadReference.set(thread);
  }

  public void terminate() {
    Thread thread = gameThreadReference.get();
    if (thread != null) {
      thread.interrupt();
    }
  }
}

package nu.borjessons.airhockeyserver.engine;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.model.GameId;

/**
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

  static Position mirror(Position position) {
    return new Position(1 - position.x(), 1 - position.y());
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

  public void updatePlayerOneHandle(Position position) {
    boardStateReference.updateAndGet(currentBoardState ->
        new BoardState(currentBoardState.puck(), new GameObject(position, currentBoardState.playerOne().speed()), currentBoardState.playerTwo())
    );
  }

  public void updatePlayerTwoHandle(Position position) {
    boardStateReference.updateAndGet(currentBoardState ->
        new BoardState(currentBoardState.puck(), currentBoardState.playerOne(), new GameObject(mirror(position), currentBoardState.playerTwo().speed()))
    );
  }
}

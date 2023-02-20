package nu.borjessons.airhockeyserver.engine;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.model.GameId;

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

  private static Handle updateHandlePosition(Supplier<Handle> handleSupplier, Position newPosition) {
    Handle handle = handleSupplier.get();
    handle.setPosition(newPosition);
    return handle;
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
        new BoardState(currentBoardState.puck(),
            updateHandlePosition(currentBoardState::playerOne, position),
            currentBoardState.playerTwo())
    );
  }

  public void updatePlayerTwoHandle(Position position) {
    boardStateReference.updateAndGet(currentBoardState ->
        new BoardState(
            currentBoardState.puck(),
            currentBoardState.playerOne(),
            updateHandlePosition(currentBoardState::playerTwo, mirror(position)))
    );
  }
}

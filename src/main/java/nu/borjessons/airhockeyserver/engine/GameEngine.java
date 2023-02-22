package nu.borjessons.airhockeyserver.engine;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.engine.properties.Position;
import nu.borjessons.airhockeyserver.model.GameId;

public class GameEngine {
  private final AtomicReference<BoardState> boardStateReference;
  private final ThreadHolder threadHolder;

  private GameEngine(AtomicReference<BoardState> boardStateReference, ThreadHolder threadHolder) {
    this.boardStateReference = boardStateReference;
    this.threadHolder = threadHolder;
  }

  public static GameEngine create() {
    AtomicReference<BoardState> atomicReference = new AtomicReference<>(GameConstants.createInitialGameState());
    return new GameEngine(atomicReference, new ThreadHolder());
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
    threadHolder.setThread(thread);
  }

  public void terminate() {
    threadHolder.getThread().ifPresent(Thread::interrupt);
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

package nu.borjessons.airhockeyserver.engine;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.model.GameId;

/**
 * It seems handle updates cannot be made with such speed because it never gives the puck a chance to update. Set handle updating to
 * occur with the same frequency as the frame rate.
 * Also, solve puck height aspect ratio for collisions.
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

  private static GameObject createNewHandlePositionAndSpeed(Position currentPosition, Position newPosition) {
    Speed speed = new Speed(newPosition.x() - currentPosition.x(), newPosition.y() - currentPosition.y());
    return new GameObject(newPosition, speed);
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
            createNewHandlePositionAndSpeed(currentBoardState.playerOne().position(), position),
            currentBoardState.playerTwo())
    );
  }

  public void updatePlayerTwoHandle(Position position) {
    boardStateReference.updateAndGet(currentBoardState ->
        new BoardState(
            currentBoardState.puck(),
            currentBoardState.playerOne(),
            createNewHandlePositionAndSpeed(currentBoardState.playerTwo().position(), mirror(position)))
    );
  }
}

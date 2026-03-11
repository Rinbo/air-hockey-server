package se.docksidelabs.airhockeyserver.game;

import java.util.function.Function;

import se.docksidelabs.airhockeyserver.game.objects.Handle;
import se.docksidelabs.airhockeyserver.game.properties.GameConstants;
import se.docksidelabs.airhockeyserver.game.properties.Position;
import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.repository.GameStoreConnector;

public class GameEngine {
  private boolean aiMode;
  private final BoardState boardState;
  private volatile Thread gameThread;

  private GameEngine(BoardState boardState) {
    this.boardState = boardState;
  }

  public static GameEngine create() {
    return new GameEngine(GameConstants.createInitialGameState());
  }

  public static Position mirror(Position position) {
    return new Position(1 - position.x(), 1 - position.y());
  }

  public void setAiMode(boolean aiMode) {
    this.aiMode = aiMode;
  }

  public void startGame(GameId gameId, GameStoreConnector gameStoreConnector) {
    if (gameThread != null && gameThread.isAlive())
      throw new IllegalStateException("Game already running");

    boardState.resetObjects();

    gameThread = Thread.ofVirtual()
        .name("game-" + gameId)
        .start(new GameRunnable(boardState, gameId, gameStoreConnector, aiMode));
  }

  public void terminate() {
    if (gameThread != null) {
      gameThread.interrupt();
      gameThread = null;
    }
  }

  public void updateHandle(Function<BoardState, Handle> function, Position position) {
    function.apply(boardState).setPosition(position);
  }
}

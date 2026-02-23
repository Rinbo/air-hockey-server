package nu.borjessons.airhockeyserver.game;

import java.util.function.Function;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.repository.GameStoreConnector;

public class GameEngine {
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

  public void startGame(GameId gameId, GameStoreConnector gameStoreConnector) {
    if (gameThread != null)
      throw new IllegalStateException("Game already running");

    gameThread = Thread.ofVirtual()
        .name("game-" + gameId)
        .start(new GameRunnable(boardState, gameId, gameStoreConnector));
  }

  public void terminate() {
    if (gameThread != null)
      gameThread.interrupt();
  }

  public void updateHandle(Function<BoardState, Handle> function, Position position) {
    function.apply(boardState).setPosition(position);
  }
}

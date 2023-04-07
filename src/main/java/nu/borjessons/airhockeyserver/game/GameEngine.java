package nu.borjessons.airhockeyserver.game;

import java.util.concurrent.Executors;
import java.util.function.Function;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.repository.GameStoreConnector;

public class GameEngine {
  private final BoardState boardState;
  private final ThreadHolder threadHolder;

  private GameEngine(BoardState boardState, ThreadHolder threadHolder) {
    this.boardState = boardState;
    this.threadHolder = threadHolder;
  }

  public static GameEngine create() {
    return new GameEngine(GameConstants.createInitialGameState(), new ThreadHolder());
  }

  public static Position mirror(Position position) {
    return new Position(1 - position.x(), 1 - position.y());
  }

  public void startGame(GameId gameId, GameStoreConnector gameStoreConnector) {
    Thread thread = new Thread(new GameRunnable(boardState, gameId, gameStoreConnector, Executors.newSingleThreadScheduledExecutor()));
    thread.start();
    threadHolder.setThread(thread);
  }

  public void terminate() {
    threadHolder.getThread().ifPresent(Thread::interrupt);
  }

  public void updateHandle(Function<BoardState, Handle> function, Position position) {
    function.apply(boardState).setPosition(position);
  }
}

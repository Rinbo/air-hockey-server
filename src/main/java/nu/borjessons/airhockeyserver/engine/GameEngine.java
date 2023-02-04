package nu.borjessons.airhockeyserver.engine;

/**
 * Start simple. Only render the two player's handles at first
 */
public class GameEngine {
  public static GameEngine init() {
    return new GameEngine(GameConstants.createInitialGameState());
  }

  private final GameState gameState;

  private GameEngine(GameState gameState) {
    this.gameState = gameState;
  }
}

package nu.borjessons.airhockeyserver.engine;

/**
 * Based on HTML Canvas default coordinate system where the origin is in the top left corner
 */
public final class GameConstants {
  static final double BOARD_ASPECT_RATIO = 1.6; // height / width
  static final double GOAL_WIDTH = 0.2;
  static final double HANDLE_RADIUS = 0.1;
  static final double PUCK_RADIUS = 0.08;
  static final Position PUCK_START_P2 = new Position(0.5, 0.4);
  static final Speed ZERO_SPEED = new Speed(0, 0);
  private static final Position HANDLE_START_P1 = new Position(0.5, .8);
  private static final Position HANDLE_START_P2 = new Position(0.5, .2);
  private static final Position PUCK_START_P1 = new Position(0.5, 0.6);

  static GameState createInitialGameState() {
    return new GameState(
        new GameObjectState(GameConstants.PUCK_START_P1, GameConstants.ZERO_SPEED),
        new GameObjectState(HANDLE_START_P1, GameConstants.ZERO_SPEED),
        new GameObjectState(HANDLE_START_P2, GameConstants.ZERO_SPEED));
  }

  private GameConstants() {
    throw new IllegalStateException();
  }
}

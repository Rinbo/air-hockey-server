package nu.borjessons.airhockeyserver.engine;

/**
 * Based on HTML Canvas default coordinate system where the origin is in the top left corner
 */
final class GameConstants {
  static final double BOARD_ASPECT_RATIO = 1.6; // height / width
  static final int FRAME_RATE = 30;
  static final double GOAL_WIDTH = 0.2;
  static final double HANDLE_RADIUS = 0.1;
  static final Position PUCK_START_P2 = new Position(0.5, 0.4);
  static final double PUCK_W_RADIUS = 0.08;
  static final double PUCK_HANDLE_MIN_DISTANCE = PUCK_W_RADIUS + HANDLE_RADIUS;
  static final double PUCK_H_RADIUS = PUCK_W_RADIUS * BOARD_ASPECT_RATIO;
  static final Speed ZERO_SPEED = new Speed(0, 0);
  private static final Position HANDLE_START_P1 = new Position(0.5, 0.8);
  private static final Position HANDLE_START_P2 = new Position(0.5, 0.2);
  private static final Position PUCK_START_P1 = new Position(0.5, 0.6);

  private GameConstants() {
    throw new IllegalStateException();
  }

  static BoardState createInitialGameState() {
    return new BoardState(
        new GameObject(GameConstants.PUCK_START_P1, GameConstants.ZERO_SPEED),
        new GameObject(HANDLE_START_P1, GameConstants.ZERO_SPEED),
        new GameObject(HANDLE_START_P2, GameConstants.ZERO_SPEED));
  }
}

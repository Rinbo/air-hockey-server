package nu.borjessons.airhockeyserver.engine;

/**
 * Based on HTML Canvas default coordinate system where the origin is in the top left corner
 */
final class GameConstants {
  static final double BOARD_ASPECT_RATIO = 0.625;
  static final int FRAME_RATE = 40;
  static final Size HANDLE_RADIUS = new Size(0.1, 0.1 * BOARD_ASPECT_RATIO);
  static final Position HANDLE_START_P1 = new Position(0.5, 0.8);
  static final Position HANDLE_START_P2 = new Position(0.5, 0.2);
  static final Size PUCK_RADIUS = new Size(0.08, 0.08 * BOARD_ASPECT_RATIO);
  static final double PUCK_HANDLE_MIN_DISTANCE = PUCK_RADIUS.x() + HANDLE_RADIUS.x();
  static final Speed ZERO_SPEED = new Speed(0, 0);
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

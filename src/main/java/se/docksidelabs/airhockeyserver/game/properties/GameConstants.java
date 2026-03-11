package se.docksidelabs.airhockeyserver.game.properties;

import java.time.Duration;

import se.docksidelabs.airhockeyserver.game.BoardState;
import se.docksidelabs.airhockeyserver.game.objects.Handle;
import se.docksidelabs.airhockeyserver.game.objects.Puck;

/**
 * Based on HTML Canvas default coordinate system where the origin is in the top
 * left corner
 * Must be kept in sync with frontend canvas rendering
 */
public final class GameConstants {
  public static final double BOARD_ASPECT_RATIO = 0.625;
  public static final int FRAME_RATE = 60;
  public static final double FRICTION_DAMPING = 0.9975;
  public static final Duration GAME_DURATION = Duration.ofSeconds(20);
  public static final double GOAL_WIDTH = 0.18;
  public static final double HANDLE_RESTITUTION = 0.9;
  // Max handle displacement per update. Prevents teleportation on fast swipes
  // and acts as server-side anti-cheat. Calibrated to ~5000 px/sec at 60 Hz.
  public static final double MAX_HANDLE_DISTANCE = 0.08;
  public static final Radius HANDLE_RADIUS = new Radius(0.09, 0.09 * BOARD_ASPECT_RATIO);
  public static final Position HANDLE_START_P1 = new Position(0.5, 0.8);
  public static final Position HANDLE_START_P2 = new Position(0.5, 0.2);
  // Max puck speed per tick — frame-rate-independent.
  // Physical max speed is MAX_SPEED * FRAME_RATE ≈ 2.95 board-widths/sec.
  public static final double MAX_SPEED = (50.0 / 1200.0 * Math.sqrt(2)) * (50.0 / FRAME_RATE);
  public static final Position OFF_BOARD_POSITION = new Position(-1, -1);
  public static final Radius PUCK_RADIUS = new Radius(0.06, 0.06 * BOARD_ASPECT_RATIO);
  public static final double PUCK_HANDLE_MIN_DISTANCE = PUCK_RADIUS.x() + HANDLE_RADIUS.x();
  public static final Duration PUCK_RESET_DURATION = Duration.ofSeconds(1);
  public static final Position PUCK_START_P1 = new Position(0.5, 0.6);
  public static final Position PUCK_START_P2 = new Position(0.5, 0.4);
  public static final double WALL_RESTITUTION = 0.85;
  public static final Speed ZERO_SPEED = new Speed(0, 0);

  private GameConstants() {
    throw new IllegalStateException();
  }

  public static BoardState createInitialGameState() {
    return new BoardState(Puck.create(GameConstants.PUCK_START_P1), Handle.create(HANDLE_START_P1),
        Handle.create(HANDLE_START_P2));
  }
}

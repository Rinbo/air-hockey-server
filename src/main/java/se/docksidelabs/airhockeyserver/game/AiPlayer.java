package se.docksidelabs.airhockeyserver.game;

import se.docksidelabs.airhockeyserver.game.objects.Handle;
import se.docksidelabs.airhockeyserver.game.objects.Puck;
import se.docksidelabs.airhockeyserver.game.properties.GameConstants;
import se.docksidelabs.airhockeyserver.game.properties.Position;

/**
 * Server-side AI opponent that controls Player 2's handle.
 *
 * <p>Strategy:
 * <ul>
 *   <li>Track the puck laterally (X axis)</li>
 *   <li>Attack aggressively when the puck is in the AI's half (y &lt; 0.5)</li>
 *   <li>Hold a defensive position when the puck is in the opponent's half</li>
 *   <li>Smooth movement via linear interpolation to prevent teleporting</li>
 *   <li>Avoid chasing the puck into side walls (handle is wider than puck)</li>
 * </ul>
 *
 * <p>Coordinates are in Player 1's frame (top=0, bottom=1). Player 2's
 * handle occupies the top half (y ≤ 0.5).
 */
public final class AiPlayer {

  private static final double DEFENSIVE_Y = 0.15;
  private static final double ATTACK_THRESHOLD_Y = 0.45;

  // Lerp factor per tick — frame-rate-independent.
  // Original tuning: 0.12 at 50 FPS. Adjusted: 1 - (1-0.12)^(50/60) ≈ 0.1007
  private static final double LERP_SPEED = 1.0 - Math.pow(1.0 - 0.12, 50.0 / GameConstants.FRAME_RATE);

  private static final double MAX_Y = 0.48;
  private static final double MIN_Y = GameConstants.HANDLE_RADIUS.y();
  private static final double MIN_X = GameConstants.HANDLE_RADIUS.x();
  private static final double MAX_X = 1.0 - GameConstants.HANDLE_RADIUS.x();

  // When puck is this close to a side wall, the AI holds center X
  // instead of chasing — the handle can't fit between puck and wall.
  private static final double WALL_MARGIN =
      GameConstants.PUCK_RADIUS.x() + GameConstants.HANDLE_RADIUS.x() + 0.02;

  // Trapped-puck detection — only fires when the AI is sandwiching
  // the puck against a wall (puck stopped + touching wall + AI close).
  private static final double STUCK_SPEED_THRESHOLD = 0.005;
  private static final double STUCK_DISTANCE_THRESHOLD =
      GameConstants.PUCK_HANDLE_MIN_DISTANCE + 0.04;

  // When the puck is closer to the goal than this, approach from below
  // instead of behind to avoid pushing it into the top wall.
  private static final double TOP_WALL_DANGER_ZONE =
      GameConstants.PUCK_RADIUS.y() + GameConstants.HANDLE_RADIUS.y() + 0.03;

  // Wall proximity tolerance for isPuckAgainstWall check
  private static final double WALL_TOLERANCE = 0.005;

  private AiPlayer() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Computes and applies the new AI handle position for the current frame.
   */
  public static void tick(BoardState boardState) {
    Handle handle = boardState.playerTwo();
    Puck puck = boardState.puck();
    Position puckPosition = puck.getPosition();
    Position currentPosition = handle.getPosition();

    if (puckPosition.equals(GameConstants.OFF_BOARD_POSITION)) {
      handle.setPosition(lerp(currentPosition, 0.5, DEFENSIVE_Y));
      return;
    }

    double puckSpeed = magnitude(puck.getSpeedX(), puck.getSpeedY());
    double distanceToPuck = distance(currentPosition, puckPosition);

    if (isPuckTrappedAgainstWall(puckPosition, puckSpeed, distanceToPuck)) {
      handle.setPosition(lerp(currentPosition, 0.5, DEFENSIVE_Y));
      return;
    }

    if (isStationaryPuckInOpenSpace(puckPosition, puckSpeed)) {
      handle.setPosition(lerp(currentPosition,
          clampX(puckPosition.x()),
          clampY(puckPosition.y())));
      return;
    }

    Position target = computeTargetPosition(puckPosition);
    handle.setPosition(lerp(currentPosition, target.x(), target.y()));
  }

  // ── Strategy ─────────────────────────────────────────────────────

  private static Position computeTargetPosition(Position puckPosition) {
    double targetX;
    double targetY;

    if (puckPosition.y() < ATTACK_THRESHOLD_Y) {
      targetX = computeAttackX(puckPosition);
      targetY = computeAttackY(puckPosition);
    } else {
      targetX = puckPosition.x();
      targetY = DEFENSIVE_Y;
    }

    return new Position(clampX(targetX), clampY(targetY));
  }

  private static double computeAttackX(Position puckPosition) {
    boolean nearSideWall = puckPosition.x() < WALL_MARGIN
        || puckPosition.x() > 1.0 - WALL_MARGIN;

    return nearSideWall ? 0.5 : puckPosition.x();
  }

  private static double computeAttackY(Position puckPosition) {
    if (puckPosition.y() < TOP_WALL_DANGER_ZONE) {
      // Near goal edge — approach from below to push puck toward opponent
      return puckPosition.y() + GameConstants.HANDLE_RADIUS.y();
    }

    // Default — position behind puck (closer to own goal)
    return Math.max(MIN_Y, puckPosition.y() - GameConstants.HANDLE_RADIUS.y());
  }

  // ── Stuck Detection ──────────────────────────────────────────────

  private static boolean isPuckTrappedAgainstWall(Position puckPosition,
                                                   double puckSpeed,
                                                   double distanceToPuck) {
    return puckSpeed < STUCK_SPEED_THRESHOLD
        && distanceToPuck < STUCK_DISTANCE_THRESHOLD
        && isPuckAgainstWall(puckPosition);
  }

  private static boolean isStationaryPuckInOpenSpace(Position puckPosition,
                                                      double puckSpeed) {
    return puckSpeed < STUCK_SPEED_THRESHOLD
        && puckPosition.y() < ATTACK_THRESHOLD_Y;
  }

  private static boolean isPuckAgainstWall(Position puckPosition) {
    double puckX = puckPosition.x();
    double puckY = puckPosition.y();
    double radiusX = GameConstants.PUCK_RADIUS.x();
    double radiusY = GameConstants.PUCK_RADIUS.y();

    if (puckX - radiusX <= WALL_TOLERANCE || puckX + radiusX >= 1.0 - WALL_TOLERANCE) {
      return true;
    }

    boolean inGoalZone = puckX >= 0.5 - GameConstants.GOAL_WIDTH
        && puckX <= 0.5 + GameConstants.GOAL_WIDTH;

    return !inGoalZone && puckY - radiusY <= WALL_TOLERANCE;
  }

  // ── Math Utilities ───────────────────────────────────────────────

  private static Position lerp(Position current, double targetX, double targetY) {
    double newX = current.x() + (targetX - current.x()) * LERP_SPEED;
    double newY = current.y() + (targetY - current.y()) * LERP_SPEED;
    return new Position(newX, newY);
  }

  private static double clampX(double x) {
    return Math.max(MIN_X, Math.min(MAX_X, x));
  }

  private static double clampY(double y) {
    return Math.max(MIN_Y, Math.min(MAX_Y, y));
  }

  private static double magnitude(double x, double y) {
    return Math.sqrt(x * x + y * y);
  }

  private static double distance(Position a, Position b) {
    double dx = a.x() - b.x();
    double dy = a.y() - b.y();
    return Math.sqrt(dx * dx + dy * dy);
  }
}

package se.docksidelabs.airhockeyserver.game;

import se.docksidelabs.airhockeyserver.game.objects.Handle;
import se.docksidelabs.airhockeyserver.game.objects.Puck;
import se.docksidelabs.airhockeyserver.game.properties.GameConstants;
import se.docksidelabs.airhockeyserver.game.properties.Position;

/**
 * Server-side AI opponent that controls Player 2's handle.
 * Called each frame from {@link GameRunnable} to compute the AI handle
 * position.
 *
 * <p>
 * Strategy:
 * <ul>
 * <li>Track the puck laterally (X axis)</li>
 * <li>Move aggressively toward the puck when it's in the AI's half (y &lt;
 * 0.5)</li>
 * <li>Hold a defensive position when the puck is in the opponent's half</li>
 * <li>Smooth movement via linear interpolation to prevent teleporting</li>
 * </ul>
 *
 * <p>
 * Coordinates are in Player 1's frame (top=0, bottom=1). Player 2's handle
 * occupies the top half (y ≤ 0.5). The AI's "home" position is near its goal.
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

    // Wall proximity threshold — when the puck is this close to a side wall,
    // the AI offsets its approach to push the puck toward center.
    private static final double WALL_MARGIN = GameConstants.PUCK_RADIUS.x() + GameConstants.HANDLE_RADIUS.x() + 0.02;

    // Trapped-puck detection — only fires when the AI is sandwiching
    // the puck against a wall (puck stopped + touching wall + AI close).
    private static final double STUCK_SPEED_THRESHOLD = 0.005;
    private static final double STUCK_DISTANCE_THRESHOLD = GameConstants.PUCK_HANDLE_MIN_DISTANCE + 0.04;

    private AiPlayer() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Computes the new AI handle position for the current frame.
     *
     * @param boardState the current board state
     */
    public static void tick(BoardState boardState) {
        Handle handle = boardState.playerTwo();
        Puck puck = boardState.puck();
        Position puckPos = puck.getPosition();
        Position currentPos = handle.getPosition();

        // Ignore puck when it's off-board (after scoring)
        if (puckPos.equals(GameConstants.OFF_BOARD_POSITION)) {
            // Move back to center defense
            double targetX = 0.5;
            double targetY = DEFENSIVE_Y;
            handle.setPosition(lerp(currentPos, targetX, targetY));
            return;
        }

        // --- Trapped-puck retreat ---
        // Only disengage when the AI is actively sandwiching the puck
        // against a wall. A puck sitting in open space (e.g. after reset)
        // must NOT trigger a retreat.
        double puckSpeed = Math.sqrt(puck.getSpeedX() * puck.getSpeedX()
                + puck.getSpeedY() * puck.getSpeedY());
        double distToPuck = Math.sqrt(
                (currentPos.x() - puckPos.x()) * (currentPos.x() - puckPos.x())
                        + (currentPos.y() - puckPos.y()) * (currentPos.y() - puckPos.y()));

        if (puckSpeed < STUCK_SPEED_THRESHOLD
                && distToPuck < STUCK_DISTANCE_THRESHOLD
                && isPuckAgainstWall(puckPos)) {
            // Puck is trapped against a wall — back away to release it
            handle.setPosition(lerp(currentPos, 0.5, DEFENSIVE_Y));
            return;
        }

        double targetX;
        double targetY;

        if (puckPos.y() < ATTACK_THRESHOLD_Y) {
            // Puck is in AI's half — attack it.
            targetX = puckPos.x();

            // Default: approach from behind (closer to own goal)
            targetY = Math.max(MIN_Y, puckPos.y() - GameConstants.HANDLE_RADIUS.y());

            // Near TOP wall: the puck is close to our goal edge. Don't get
            // behind it (that pushes it into the wall). Instead approach from
            // BELOW (higher Y) to push it toward the opponent.
            double topWallMargin = GameConstants.PUCK_RADIUS.y() + GameConstants.HANDLE_RADIUS.y() + 0.03;
            if (puckPos.y() < topWallMargin) {
                targetY = puckPos.y() + GameConstants.HANDLE_RADIUS.y();
            }

            // Near SIDE walls: DON'T chase the puck to the wall.
            // The handle (radius 0.09) is wider than the puck (radius 0.06),
            // so it can never fit between puck and wall — any contact near
            // a wall pushes the puck INTO the wall.  Instead, hold center X
            // and wait for the puck to bounce back into open space.
            if (puckPos.x() < WALL_MARGIN || puckPos.x() > 1.0 - WALL_MARGIN) {
                targetX = 0.5;
            }
        } else {
            // Puck is in opponent's half — hold defensive position, track X loosely
            targetX = puckPos.x();
            targetY = DEFENSIVE_Y;
        }

        // Clamp to valid bounds
        targetX = Math.max(MIN_X, Math.min(MAX_X, targetX));
        targetY = Math.max(MIN_Y, Math.min(MAX_Y, targetY));

        handle.setPosition(lerp(currentPos, targetX, targetY));
    }

    /**
     * Returns {@code true} when the puck is touching (or nearly touching) a
     * wall boundary. Used to distinguish a genuinely trapped puck from one
     * that is simply stationary in open space (e.g. after a goal reset).
     */
    private static boolean isPuckAgainstWall(Position puckPos) {
        double px = puckPos.x();
        double py = puckPos.y();
        double rx = GameConstants.PUCK_RADIUS.x();
        double ry = GameConstants.PUCK_RADIUS.y();
        double tolerance = 0.005;

        // Near left or right wall
        if (px - rx <= tolerance || px + rx >= 1.0 - tolerance) return true;

        // Near top wall (only outside the goal opening)
        boolean inGoalZone = px >= 0.5 - GameConstants.GOAL_WIDTH
                          && px <= 0.5 + GameConstants.GOAL_WIDTH;
        if (!inGoalZone && py - ry <= tolerance) return true;

        return false;
    }

    private static Position lerp(Position current, double targetX, double targetY) {
        double newX = current.x() + (targetX - current.x()) * LERP_SPEED;
        double newY = current.y() + (targetY - current.y()) * LERP_SPEED;
        return new Position(newX, newY);
    }
}

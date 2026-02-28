package nu.borjessons.airhockeyserver.game;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;
import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;

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
    private static final double LERP_SPEED = 0.12;
    private static final double MAX_Y = 0.48;
    private static final double MIN_Y = GameConstants.HANDLE_RADIUS.y();
    private static final double MIN_X = GameConstants.HANDLE_RADIUS.x();
    private static final double MAX_X = 1.0 - GameConstants.HANDLE_RADIUS.x();

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

        double targetX;
        double targetY;

        if (puckPos.y() < ATTACK_THRESHOLD_Y) {
            // Puck is in AI's half — move aggressively toward it
            targetX = puckPos.x();
            // Stay slightly behind the puck (closer to own goal) for better defense
            targetY = Math.max(MIN_Y, puckPos.y() - GameConstants.HANDLE_RADIUS.y());
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

    private static Position lerp(Position current, double targetX, double targetY) {
        double newX = current.x() + (targetX - current.x()) * LERP_SPEED;
        double newY = current.y() + (targetY - current.y()) * LERP_SPEED;
        return new Position(newX, newY);
    }
}

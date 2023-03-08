package nu.borjessons.airhockeyserver.game.properties;

import nu.borjessons.airhockeyserver.game.BoardState;
import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;

/**
 * Based on HTML Canvas default coordinate system where the origin is in the top left corner
 */
public final class GameConstants {
    public static final double BOARD_ASPECT_RATIO = 0.625;
    public static final int FRAME_RATE = 60;
    public static final Radius HANDLE_RADIUS = new Radius(0.1, 0.1 * BOARD_ASPECT_RATIO);
    public static final Position HANDLE_START_P1 = new Position(0.5, 0.8);
    public static final Position HANDLE_START_P2 = new Position(0.5, 0.2);
    public static final Radius PUCK_RADIUS = new Radius(0.08, 0.08 * BOARD_ASPECT_RATIO);
    public static final double PUCK_HANDLE_MIN_DISTANCE = PUCK_RADIUS.x() + HANDLE_RADIUS.x();
    public static final Speed ZERO_SPEED = new Speed(0, 0);
    public static final double FRICTION_MODIFIER = 20_000;
    public static final double GOAL_WIDTH = 0.15;
    private static final Position PUCK_START_P1 = new Position(0.5, 0.6);

    private GameConstants() {
        throw new IllegalStateException();
    }

    public static BoardState createInitialGameState() {
        return new BoardState(Puck.create(GameConstants.PUCK_START_P1), Handle.create(HANDLE_START_P1), Handle.create(HANDLE_START_P2));
    }
}

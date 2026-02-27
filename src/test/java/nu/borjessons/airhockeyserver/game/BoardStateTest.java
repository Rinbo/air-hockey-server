package nu.borjessons.airhockeyserver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;
import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Speed;

/**
 * Tests BoardState.resetObjects() which is called after every goal and at game
 * end.
 * If this breaks, objects end up in wrong positions between rounds.
 */
@DisplayName("BoardState")
class BoardStateTest {

    private static final double DELTA = 1e-10;

    @Test
    @DisplayName("resetObjects places puck at alternating start positions")
    void resetObjectsAlternatesPuckPosition() {
        Puck puck = Puck.create(new Position(0.1, 0.1));
        Handle p1 = Handle.create(new Position(0.3, 0.3));
        Handle p2 = Handle.create(new Position(0.7, 0.7));
        BoardState boardState = new BoardState(puck, p1, p2);

        // First reset — atomic counter is 1, which is odd → P2 start
        boardState.resetObjects();
        assertEquals(GameConstants.PUCK_START_P2, puck.getPosition(),
                "First reset should place puck at P2 start position");

        // Second reset — counter is 2, which is even → P1 start
        boardState.resetObjects();
        assertEquals(GameConstants.PUCK_START_P1, puck.getPosition(),
                "Second reset should place puck at P1 start position");

        // Third reset — counter is 3, which is odd → P2 start
        boardState.resetObjects();
        assertEquals(GameConstants.PUCK_START_P2, puck.getPosition(),
                "Third reset should alternate back to P2 start position");
    }

    @Test
    @DisplayName("resetObjects zeroes puck speed")
    void resetObjectsZeroesPuckSpeed() {
        Puck puck = Puck.create(new Position(0.5, 0.5));
        puck.setSpeedXY(0.1, 0.2);
        Handle p1 = Handle.create(GameConstants.HANDLE_START_P1);
        Handle p2 = Handle.create(GameConstants.HANDLE_START_P2);
        BoardState boardState = new BoardState(puck, p1, p2);

        boardState.resetObjects();

        Speed speed = puck.getSpeed();
        assertEquals(0, speed.x(), DELTA, "Puck X speed should be zeroed after reset");
        assertEquals(0, speed.y(), DELTA, "Puck Y speed should be zeroed after reset");
    }

    @Test
    @DisplayName("resetObjects places handles at their starting positions")
    void resetObjectsResetsHandles() {
        Handle p1 = Handle.create(new Position(0.1, 0.1));
        Handle p2 = Handle.create(new Position(0.9, 0.9));
        BoardState boardState = new BoardState(Puck.create(new Position(0.5, 0.5)), p1, p2);

        boardState.resetObjects();

        assertEquals(GameConstants.HANDLE_START_P1, p1.getPosition(),
                "Player 1 handle should be reset to start position");
        assertEquals(GameConstants.HANDLE_START_P2, p2.getPosition(),
                "Player 2 handle should be reset to start position");
    }
}

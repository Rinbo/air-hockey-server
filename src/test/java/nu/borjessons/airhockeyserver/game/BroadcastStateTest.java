package nu.borjessons.airhockeyserver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.game.properties.Position;

/**
 * Tests BroadcastState coordinate mirroring.
 * The mirroring logic is critical — if it breaks, Player 2 sees the board
 * upside down or the opponent/puck in the wrong position.
 */
@DisplayName("BroadcastState")
class BroadcastStateTest {

    private static final double DELTA = 1e-10;

    @Test
    @DisplayName("set() stores exact coordinates")
    void setStoresExactCoordinates() {
        BroadcastState state = new BroadcastState();
        state.set(new Position(0.3, 0.7), new Position(0.5, 0.2), 60, BroadcastState.NO_EVENT);

        assertEquals(0.3, state.getOpponent().getX(), DELTA);
        assertEquals(0.7, state.getOpponent().getY(), DELTA);
        assertEquals(0.5, state.getPuck().getX(), DELTA);
        assertEquals(0.2, state.getPuck().getY(), DELTA);
        assertEquals(60, state.getRemainingSeconds());
    }

    @Test
    @DisplayName("setMirrored() inverts coordinates via (1-x, 1-y)")
    void setMirroredInvertsCoordinates() {
        BroadcastState state = new BroadcastState();
        state.setMirrored(new Position(0.3, 0.7), new Position(0.5, 0.2), 45, BroadcastState.NO_EVENT);

        assertEquals(0.7, state.getOpponent().getX(), DELTA);
        assertEquals(0.3, state.getOpponent().getY(), DELTA);
        assertEquals(0.5, state.getPuck().getX(), DELTA);
        assertEquals(0.8, state.getPuck().getY(), DELTA);
        assertEquals(45, state.getRemainingSeconds());
    }

    @Test
    @DisplayName("Mirroring center position (0.5, 0.5) returns (0.5, 0.5)")
    void mirroringCenterIsIdentity() {
        BroadcastState state = new BroadcastState();
        Position center = new Position(0.5, 0.5);
        state.setMirrored(center, center, 30, BroadcastState.NO_EVENT);

        assertEquals(0.5, state.getOpponent().getX(), DELTA);
        assertEquals(0.5, state.getOpponent().getY(), DELTA);
        assertEquals(0.5, state.getPuck().getX(), DELTA);
        assertEquals(0.5, state.getPuck().getY(), DELTA);
    }

    @Test
    @DisplayName("Mirroring is self-inverse: mirror(mirror(p)) == p")
    void mirroringIsSelfInverse() {
        Position original = new Position(0.2, 0.8);

        // First mirror
        BroadcastState state1 = new BroadcastState();
        state1.setMirrored(original, original, 10, BroadcastState.NO_EVENT);

        // Second mirror
        Position mirrored = new Position(state1.getOpponent().getX(), state1.getOpponent().getY());
        BroadcastState state2 = new BroadcastState();
        state2.setMirrored(mirrored, mirrored, 10, BroadcastState.NO_EVENT);

        assertEquals(original.x(), state2.getOpponent().getX(), DELTA);
        assertEquals(original.y(), state2.getOpponent().getY(), DELTA);
    }

    @Test
    @DisplayName("Collision event is stored and retrievable")
    void collisionEventIsStored() {
        BroadcastState state = new BroadcastState();
        state.set(new Position(0.5, 0.5), new Position(0.5, 0.5), 30, BroadcastState.HANDLE_HIT);
        assertEquals(BroadcastState.HANDLE_HIT, state.getCollisionEvent());

        state.setMirrored(new Position(0.5, 0.5), new Position(0.5, 0.5), 30, BroadcastState.GOAL);
        assertEquals(BroadcastState.GOAL, state.getCollisionEvent());
    }
}

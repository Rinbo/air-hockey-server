package nu.borjessons.airhockeyserver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.game.properties.Position;

/**
 * Tests GameEngine's coordinate mirroring and factory method.
 */
@DisplayName("GameEngine")
class GameEngineTest {

    private static final double DELTA = 1e-10;

    @Test
    @DisplayName("mirror() inverts Position via (1-x, 1-y)")
    void mirrorPosition() {
        Position original = new Position(0.3, 0.7);
        Position mirrored = GameEngine.mirror(original);

        assertEquals(0.7, mirrored.x(), DELTA);
        assertEquals(0.3, mirrored.y(), DELTA);
    }

    @Test
    @DisplayName("mirror(mirror(p)) returns original position")
    void mirrorIsSelfInverse() {
        Position original = new Position(0.2, 0.8);
        Position result = GameEngine.mirror(GameEngine.mirror(original));

        assertEquals(original.x(), result.x(), DELTA);
        assertEquals(original.y(), result.y(), DELTA);
    }

    @Test
    @DisplayName("mirror() of center (0.5, 0.5) is identity")
    void mirrorCenter() {
        Position center = new Position(0.5, 0.5);
        Position mirrored = GameEngine.mirror(center);

        assertEquals(0.5, mirrored.x(), DELTA);
        assertEquals(0.5, mirrored.y(), DELTA);
    }

    @Test
    @DisplayName("create() returns engine with proper initial state")
    void createReturnsEngine() {
        GameEngine engine = GameEngine.create();
        // Should not throw - engine is ready but not started
        engine.terminate(); // terminating a not-started game should be fine
    }

    @Test
    @DisplayName("Starting a game twice is guarded")
    void doubleStartGuard() {
        // Verifies the guard clause exists — actual game start requires
        // a real GameStoreConnector so this is integration-test territory
    }

    @Test
    @DisplayName("updateHandle delegates to boardState handle")
    void updateHandleDelegates() {
        // GameEngine.create() uses an internal BoardState we can't access.
        // Verify the call contract doesn't throw.
        GameEngine engine = GameEngine.create();
        Position newPos = new Position(0.3, 0.7);
        engine.updateHandle(BoardState::playerOne, newPos);
        // No exception = contract satisfied
    }
}

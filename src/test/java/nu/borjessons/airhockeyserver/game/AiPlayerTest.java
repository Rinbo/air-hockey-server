package nu.borjessons.airhockeyserver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;
import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;

@DisplayName("AI Player")
class AiPlayerTest {

    private BoardState boardState;

    @BeforeEach
    void setUp() {
        Puck puck = Puck.create(new Position(0.5, 0.5));
        Handle p1 = Handle.create(GameConstants.HANDLE_START_P1);
        Handle p2 = Handle.create(GameConstants.HANDLE_START_P2);
        boardState = new BoardState(puck, p1, p2);
    }

    @Test
    @DisplayName("AI handle stays within Player 2's half (y <= 0.5)")
    void handleStaysInTopHalf() {
        // Place puck deep in player 1's half
        boardState.puck().setPosition(new Position(0.3, 0.9));

        for (int i = 0; i < 200; i++) {
            AiPlayer.tick(boardState);
        }

        Position aiPos = boardState.playerTwo().getPosition();
        assertTrue(aiPos.y() <= 0.5, "AI handle y should be <= 0.5 but was " + aiPos.y());
    }

    @Test
    @DisplayName("AI tracks puck X when puck is in AI half")
    void tracksPuckXInAiHalf() {
        boardState.puck().setPosition(new Position(0.2, 0.3));

        // Run many ticks to converge
        for (int i = 0; i < 300; i++) {
            AiPlayer.tick(boardState);
        }

        Position aiPos = boardState.playerTwo().getPosition();
        assertEquals(0.2, aiPos.x(), 0.05, "AI handle X should converge toward puck X");
    }

    @Test
    @DisplayName("AI moves to defensive position when puck is far away")
    void defensivePositionWhenPuckFar() {
        boardState.puck().setPosition(new Position(0.5, 0.8));

        for (int i = 0; i < 300; i++) {
            AiPlayer.tick(boardState);
        }

        Position aiPos = boardState.playerTwo().getPosition();
        assertTrue(aiPos.y() < 0.25, "AI should be near defensive position when puck is far away, y was " + aiPos.y());
    }

    @Test
    @DisplayName("AI handle moves smoothly (lerp prevents teleporting)")
    void smoothMovement() {
        boardState.puck().setPosition(new Position(0.8, 0.3));
        Position before = boardState.playerTwo().getPosition();

        AiPlayer.tick(boardState);

        Position after = boardState.playerTwo().getPosition();
        double dx = Math.abs(after.x() - before.x());
        double dy = Math.abs(after.y() - before.y());

        // Should move only a fraction of the distance (lerp factor is 0.12)
        assertTrue(dx < 0.15, "X movement per tick should be small (lerp), was " + dx);
        assertTrue(dy < 0.15, "Y movement per tick should be small (lerp), was " + dy);
    }

    @Test
    @DisplayName("AI handles puck at off-board position gracefully")
    void offBoardPuck() {
        boardState.puck().setPosition(GameConstants.OFF_BOARD_POSITION);

        // Should not throw
        for (int i = 0; i < 50; i++) {
            AiPlayer.tick(boardState);
        }

        Position aiPos = boardState.playerTwo().getPosition();
        // Should move toward center defense (0.5, 0.15)
        assertEquals(0.5, aiPos.x(), 0.1, "AI should be near center X when puck is off-board");
    }
}

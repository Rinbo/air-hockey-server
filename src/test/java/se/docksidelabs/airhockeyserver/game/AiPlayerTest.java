package se.docksidelabs.airhockeyserver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import se.docksidelabs.airhockeyserver.game.objects.Handle;
import se.docksidelabs.airhockeyserver.game.objects.Puck;
import se.docksidelabs.airhockeyserver.game.properties.GameConstants;
import se.docksidelabs.airhockeyserver.game.properties.Position;

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
        // Place puck away from walls to avoid triggering corner avoidance offset
        // Give it speed so stuck-detection doesn't trigger a retreat
        boardState.puck().setPosition(new Position(0.35, 0.3));
        boardState.puck().setSpeedXY(0.005, 0.005);

        // Run many ticks to converge
        for (int i = 0; i < 300; i++) {
            AiPlayer.tick(boardState);
        }

        Position aiPos = boardState.playerTwo().getPosition();
        assertEquals(0.35, aiPos.x(), 0.05, "AI handle X should converge toward puck X");
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

    @Test
    @DisplayName("AI approaches a reset puck (zero speed, open space)")
    void aiApproachesResetPuck() {
        // Place puck at PUCK_START_P2 with zero speed — simulates post-goal reset
        boardState.puck().setPosition(GameConstants.PUCK_START_P2); // (0.5, 0.4)
        boardState.puck().setSpeedXY(0, 0);

        double initialDist = distance(boardState.playerTwo().getPosition(),
                GameConstants.PUCK_START_P2);

        for (int i = 0; i < 300; i++) {
            AiPlayer.tick(boardState);
        }

        double finalDist = distance(boardState.playerTwo().getPosition(),
                boardState.puck().getPosition());
        assertTrue(finalDist < initialDist,
                "AI should approach the reset puck, not retreat. Initial dist="
                        + initialDist + " final dist=" + finalDist);
    }

    @Test
    @DisplayName("AI holds center when puck is near side wall")
    void aiDoesNotChasePuckToSideWall() {
        // Place puck near the left wall with some speed
        boardState.puck().setPosition(new Position(GameConstants.PUCK_RADIUS.x() + 0.02, 0.3));
        boardState.puck().setSpeedXY(0.005, 0.005);

        for (int i = 0; i < 300; i++) {
            AiPlayer.tick(boardState);
        }

        Position aiPos = boardState.playerTwo().getPosition();
        // AI should stay near center X, not chase to the wall
        assertTrue(aiPos.x() > 0.3,
                "AI should hold near center X when puck is near wall, but AI x="
                        + aiPos.x());
    }

    @Test
    @DisplayName("AI hits a stationary puck in open space (not against wall)")
    void aiHitsStationaryPuckInOpenSpace() {
        // Place puck in AI's half, away from all walls, with zero speed.
        // This reproduces the bug where the AI wobbles near the puck
        // without ever making contact.
        Position puckPos = new Position(0.3, 0.3);
        boardState.puck().setPosition(puckPos);
        boardState.puck().setSpeedXY(0, 0);

        for (int i = 0; i < 500; i++) {
            AiPlayer.tick(boardState);
        }

        double dist = distance(boardState.playerTwo().getPosition(), puckPos);
        assertTrue(dist <= GameConstants.PUCK_HANDLE_MIN_DISTANCE + 0.01,
                "AI should converge onto the stationary puck (within collision range), "
                        + "but distance was " + dist);
    }

    private static double distance(Position a, Position b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }
}

package nu.borjessons.airhockeyserver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;
import nu.borjessons.airhockeyserver.game.properties.Collision;
import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.repository.GameStoreConnector;

/**
 * Tests the collision detection logic in GameRunnable.
 * This is the most regression-sensitive code in the app — a change to any
 * GameConstants threshold (GOAL_WIDTH, PUCK_RADIUS, etc.) must cause these
 * tests to fail so the developer is forced to verify the behaviour is still
 * correct.
 */
@DisplayName("Collision Detection")
class CollisionDetectionTest {

    private GameStoreConnector connector;

    private GameRunnable createRunnable(Position puckPos) {
        return createRunnable(puckPos, GameConstants.HANDLE_START_P1, GameConstants.HANDLE_START_P2);
    }

    private GameRunnable createRunnable(Position puckPos, Position handleOnePos, Position handleTwoPos) {
        Puck puck = Puck.create(puckPos);
        Handle p1 = Handle.create(handleOnePos);
        Handle p2 = Handle.create(handleTwoPos);
        BoardState state = new BoardState(puck, p1, p2);
        return new GameRunnable(state, new GameId("test"), connector);
    }

    @BeforeEach
    void setUp() {
        connector = Mockito.mock(GameStoreConnector.class);
    }

    // ─── Wall Collisions ──────────────────────────────────────────

    @Nested
    @DisplayName("Wall collisions")
    class WallCollisions {

        @Test
        @DisplayName("Puck touching left wall → LEFT_WALL")
        void leftWall() {
            Position puckPos = new Position(GameConstants.PUCK_RADIUS.x(), 0.5);
            assertEquals(Collision.LEFT_WALL, createRunnable(puckPos).detectCollision());
        }

        @Test
        @DisplayName("Puck past left wall → LEFT_WALL")
        void leftWallPast() {
            Position puckPos = new Position(GameConstants.PUCK_RADIUS.x() - 0.01, 0.5);
            assertEquals(Collision.LEFT_WALL, createRunnable(puckPos).detectCollision());
        }

        @Test
        @DisplayName("Puck touching right wall → RIGHT_WALL")
        void rightWall() {
            Position puckPos = new Position(1.0 - GameConstants.PUCK_RADIUS.x(), 0.5);
            assertEquals(Collision.RIGHT_WALL, createRunnable(puckPos).detectCollision());
        }

        @Test
        @DisplayName("Puck past right wall → RIGHT_WALL")
        void rightWallPast() {
            Position puckPos = new Position(1.0 - GameConstants.PUCK_RADIUS.x() + 0.01, 0.5);
            assertEquals(Collision.RIGHT_WALL, createRunnable(puckPos).detectCollision());
        }

        @Test
        @DisplayName("Puck touching top wall outside goal → TOP_WALL")
        void topWall() {
            // x far from goal (goal is centered at 0.5)
            Position puckPos = new Position(0.1, GameConstants.PUCK_RADIUS.y());
            assertEquals(Collision.TOP_WALL, createRunnable(puckPos).detectCollision());
        }

        @Test
        @DisplayName("Puck touching bottom wall outside goal → BOTTOM_WALL")
        void bottomWall() {
            Position puckPos = new Position(0.1, 1.0 - GameConstants.PUCK_RADIUS.y());
            assertEquals(Collision.BOTTOM_WALL, createRunnable(puckPos).detectCollision());
        }
    }

    // ─── Goal Scoring ─────────────────────────────────────────────

    @Nested
    @DisplayName("Goal scoring")
    class GoalScoring {

        @Test
        @DisplayName("Puck past bottom edge (y exceeds 1) → P1_GOAL (player 2 scores)")
        void player1Goal() {
            // Puck center is past the bottom edge
            Position puckPos = new Position(0.5, 1.0 + GameConstants.PUCK_RADIUS.y() + 0.01);
            assertEquals(Collision.P1_GOAL, createRunnable(puckPos).detectCollision());
        }

        @Test
        @DisplayName("Puck past top edge (y below 0) → P2_GOAL (player 1 scores)")
        void player2Goal() {
            Position puckPos = new Position(0.5, -(GameConstants.PUCK_RADIUS.y() + 0.01));
            assertEquals(Collision.P2_GOAL, createRunnable(puckPos).detectCollision());
        }

        @Test
        @DisplayName("Puck in goal zone top wall should NOT trigger TOP_WALL")
        void topGoalZoneIsNotWall() {
            // Puck in the center of the goal mouth at y=0
            Position puckPos = new Position(0.5, GameConstants.PUCK_RADIUS.y());
            Collision result = createRunnable(puckPos).detectCollision();
            // Should NOT be TOP_WALL since it's in the goal opening
            assertEquals(Collision.NO_COLLISION, result,
                    "Puck at the goal mouth should pass through, not bounce off the wall");
        }

        @Test
        @DisplayName("Puck in goal zone bottom wall should NOT trigger BOTTOM_WALL")
        void bottomGoalZoneIsNotWall() {
            Position puckPos = new Position(0.5, 1.0 - GameConstants.PUCK_RADIUS.y());
            Collision result = createRunnable(puckPos).detectCollision();
            assertEquals(Collision.NO_COLLISION, result,
                    "Puck at the goal mouth should pass through, not bounce off the wall");
        }
    }

    // ─── Handle Collisions ────────────────────────────────────────

    @Nested
    @DisplayName("Handle collisions")
    class HandleCollisions {

        @Test
        @DisplayName("Puck overlapping P1 handle → P1_HANDLE")
        void player1Handle() {
            // Place puck at same Y as handle, with small X offset within collision
            // distance.
            // The distance formula normalizes Y by dividing by BOARD_ASPECT_RATIO, so using
            // a pure X offset is the most reliable way to trigger collision.
            Position handlePos = new Position(0.5, 0.8);
            double xOffset = GameConstants.PUCK_HANDLE_MIN_DISTANCE - 0.001;
            Position puckPos = new Position(0.5 + xOffset, 0.8);
            assertEquals(Collision.P1_HANDLE,
                    createRunnable(puckPos, handlePos, GameConstants.HANDLE_START_P2).detectCollision());
        }

        @Test
        @DisplayName("Puck overlapping P2 handle → P2_HANDLE")
        void player2Handle() {
            Position handlePos = new Position(0.5, 0.2);
            double xOffset = GameConstants.PUCK_HANDLE_MIN_DISTANCE - 0.001;
            Position puckPos = new Position(0.5 - xOffset, 0.2);
            assertEquals(Collision.P2_HANDLE,
                    createRunnable(puckPos, GameConstants.HANDLE_START_P1, handlePos).detectCollision());
        }

        @Test
        @DisplayName("Puck just outside handle distance → NO_COLLISION")
        void noHandleCollision() {
            Position handlePos = new Position(0.5, 0.8);
            // Place puck further than min distance on X axis
            double xOffset = GameConstants.PUCK_HANDLE_MIN_DISTANCE + 0.05;
            Position puckPos = new Position(0.5 + xOffset, 0.8);
            assertEquals(Collision.NO_COLLISION,
                    createRunnable(puckPos, handlePos, GameConstants.HANDLE_START_P2).detectCollision());
        }
    }

    // ─── Special Cases ────────────────────────────────────────────

    @Nested
    @DisplayName("Special cases")
    class SpecialCases {

        @Test
        @DisplayName("Puck off-board → NO_COLLISION")
        void offBoard() {
            assertEquals(Collision.NO_COLLISION, createRunnable(GameConstants.OFF_BOARD_POSITION).detectCollision());
        }

        @Test
        @DisplayName("Puck in center of board → NO_COLLISION")
        void centerOfBoard() {
            Position center = new Position(0.5, 0.5);
            assertEquals(Collision.NO_COLLISION, createRunnable(center).detectCollision());
        }
    }
}

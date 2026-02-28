package nu.borjessons.airhockeyserver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;
import nu.borjessons.airhockeyserver.game.properties.Collision;
import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Speed;
import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.repository.GameStoreConnector;

/**
 * Tests that collision *handling* mutates puck state correctly.
 * Collision detection is tested separately in CollisionDetectionTest.
 * <p>
 * These tests guard against regressions when modifying wall-bounce logic,
 * speed capping, or scoring.
 */
@DisplayName("Collision Handling")
class CollisionHandlingTest {

    private GameStoreConnector connector;
    private BoardState boardState;
    private GameRunnable runnable;

    @BeforeEach
    void setUp() {
        connector = Mockito.mock(GameStoreConnector.class);
        Puck puck = Puck.create(new Position(0.5, 0.5));
        Handle p1 = Handle.create(GameConstants.HANDLE_START_P1);
        Handle p2 = Handle.create(GameConstants.HANDLE_START_P2);
        boardState = new BoardState(puck, p1, p2);
        runnable = new GameRunnable(boardState, new GameId("test"), connector);
    }

    // ─── Wall Bounce Tests ────────────────────────────────────────

    @Test
    @DisplayName("Left wall collision bounces with restitution and clamps position")
    void leftWallBounce() {
        Puck puck = boardState.puck();
        puck.setSpeedXY(-0.01, 0.005);
        puck.setPosition(new Position(GameConstants.PUCK_RADIUS.x() - 0.01, 0.5));

        runnable.handleCollision(Collision.LEFT_WALL);

        assertEquals(GameConstants.PUCK_RADIUS.x(), puck.getPosition().x(),
                "Puck x should be clamped to left wall + radius");
        assertTrue(puck.getSpeedX() > 0, "X speed should be positive after left wall bounce");
        assertEquals(0.01 * GameConstants.WALL_RESTITUTION, puck.getSpeedX(), 1e-10,
                "X speed should be reduced by wall restitution");
    }

    @Test
    @DisplayName("Right wall collision bounces with restitution and clamps position")
    void rightWallBounce() {
        Puck puck = boardState.puck();
        puck.setSpeedXY(0.01, 0.005);
        puck.setPosition(new Position(1.0 - GameConstants.PUCK_RADIUS.x() + 0.01, 0.5));

        runnable.handleCollision(Collision.RIGHT_WALL);

        assertEquals(1.0 - GameConstants.PUCK_RADIUS.x(), puck.getPosition().x(),
                "Puck x should be clamped to right wall - radius");
        assertTrue(puck.getSpeedX() < 0, "X speed should be negative after right wall bounce");
        assertEquals(-0.01 * GameConstants.WALL_RESTITUTION, puck.getSpeedX(), 1e-10,
                "X speed should be reduced by wall restitution");
    }

    @Test
    @DisplayName("Top wall collision bounces with restitution and clamps position")
    void topWallBounce() {
        Puck puck = boardState.puck();
        puck.setSpeedXY(0.005, -0.01);
        puck.setPosition(new Position(0.1, GameConstants.PUCK_RADIUS.y() - 0.01));

        runnable.handleCollision(Collision.TOP_WALL);

        assertEquals(GameConstants.PUCK_RADIUS.y(), puck.getPosition().y(),
                "Puck y should be clamped to top wall + radius");
        assertTrue(puck.getSpeedY() > 0, "Y speed should be positive after top wall bounce");
        assertEquals(0.01 * GameConstants.WALL_RESTITUTION, puck.getSpeedY(), 1e-10,
                "Y speed should be reduced by wall restitution");
    }

    @Test
    @DisplayName("Bottom wall collision bounces with restitution and clamps position")
    void bottomWallBounce() {
        Puck puck = boardState.puck();
        puck.setSpeedXY(0.005, 0.01);
        puck.setPosition(new Position(0.1, 1.0 - GameConstants.PUCK_RADIUS.y() + 0.01));

        runnable.handleCollision(Collision.BOTTOM_WALL);

        assertEquals(1.0 - GameConstants.PUCK_RADIUS.y(), puck.getPosition().y(),
                "Puck y should be clamped to bottom wall - radius");
        assertTrue(puck.getSpeedY() < 0, "Y speed should be negative after bottom wall bounce");
        assertEquals(-0.01 * GameConstants.WALL_RESTITUTION, puck.getSpeedY(), 1e-10,
                "Y speed should be reduced by wall restitution");
    }

    @Test
    @DisplayName("Non-bounced axis speed is preserved (not affected by restitution)")
    void speedPreservedOnOtherAxis() {
        Puck puck = boardState.puck();
        puck.setSpeedXY(-0.01, 0.005);
        puck.setPosition(new Position(GameConstants.PUCK_RADIUS.x() - 0.01, 0.5));

        double yBefore = puck.getSpeedY();
        runnable.handleCollision(Collision.LEFT_WALL);

        assertEquals(yBefore, puck.getSpeedY(), 1e-10,
                "Y speed should be unchanged after a left wall bounce");
    }

    // ─── Scoring Tests ────────────────────────────────────────────

    @Test
    @DisplayName("P1_GOAL triggers PLAYER_2 score update and places puck off-board")
    void player1GoalScoring() {
        Puck puck = boardState.puck();
        puck.setSpeedXY(0, 0.02);

        runnable.handleCollision(Collision.P1_GOAL);

        Mockito.verify(connector).updatePlayerScore(Agency.PLAYER_2);
        assertEquals(GameConstants.OFF_BOARD_POSITION, puck.getPosition(),
                "Puck should be moved off-board after a goal");
        assertEquals(0, puck.getSpeedX(), "Puck speed should be zeroed after a goal");
        assertEquals(0, puck.getSpeedY(), "Puck speed should be zeroed after a goal");
    }

    @Test
    @DisplayName("P2_GOAL triggers PLAYER_1 score update")
    void player2GoalScoring() {
        runnable.handleCollision(Collision.P2_GOAL);

        Mockito.verify(connector).updatePlayerScore(Agency.PLAYER_1);
        assertEquals(GameConstants.OFF_BOARD_POSITION, boardState.puck().getPosition());
    }

    @Test
    @DisplayName("Scoring agency mapping: P1_GOAL → PLAYER_2, P2_GOAL → PLAYER_1")
    void scoringAgencyMapping() {
        // This tests the counterintuitive mapping: ball going into P1's goal = P2
        // scores
        GameStoreConnector c1 = Mockito.mock(GameStoreConnector.class);
        GameStoreConnector c2 = Mockito.mock(GameStoreConnector.class);

        Puck puck1 = Puck.create(new Position(0.5, 0.5));
        BoardState bs1 = new BoardState(puck1, Handle.create(GameConstants.HANDLE_START_P1),
                Handle.create(GameConstants.HANDLE_START_P2));
        new GameRunnable(bs1, new GameId("t1"), c1).handleCollision(Collision.P1_GOAL);

        Puck puck2 = Puck.create(new Position(0.5, 0.5));
        BoardState bs2 = new BoardState(puck2, Handle.create(GameConstants.HANDLE_START_P1),
                Handle.create(GameConstants.HANDLE_START_P2));
        new GameRunnable(bs2, new GameId("t2"), c2).handleCollision(Collision.P2_GOAL);

        Mockito.verify(c1).updatePlayerScore(Agency.PLAYER_2);
        Mockito.verify(c2).updatePlayerScore(Agency.PLAYER_1);
    }

    // ─── Handle Collision Tests ───────────────────────────────────

    @Test
    @DisplayName("Handle collision changes puck speed")
    void handleCollisionChangesPuckSpeed() {
        Puck puck = boardState.puck();
        Speed speedBefore = puck.getSpeed();

        // Move puck right next to P1 handle for a valid handle collision
        Position handlePos = boardState.playerOne().getPosition();
        Position nearHandle = new Position(handlePos.x(),
                handlePos.y() - GameConstants.PUCK_HANDLE_MIN_DISTANCE + 0.001);
        puck.setPosition(nearHandle);
        puck.setSpeedXY(0.005, -0.005);

        runnable.handleCollision(Collision.P1_HANDLE);

        Speed speedAfter = puck.getSpeed();
        // Speed should change in some way after a handle collision
        assertNotEquals(speedBefore, speedAfter,
                "Puck speed should change after a handle collision");
    }

    // ─── NO_COLLISION ─────────────────────────────────────────────

    @Test
    @DisplayName("NO_COLLISION does not mutate puck state")
    void noCollisionNoMutation() {
        Puck puck = boardState.puck();
        puck.setSpeedXY(0.005, 0.005);
        Position posBefore = puck.getPosition();
        Speed speedBefore = puck.getSpeed();

        runnable.handleCollision(Collision.NO_COLLISION);

        assertEquals(posBefore, puck.getPosition(), "Position should not change on NO_COLLISION");
        assertEquals(speedBefore, puck.getSpeed(), "Speed should not change on NO_COLLISION");
        Mockito.verifyNoInteractions(connector);
    }
}

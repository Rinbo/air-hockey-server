package nu.borjessons.airhockeyserver.game.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Speed;
import nu.borjessons.airhockeyserver.game.properties.Vector;

/**
 * Tests the physics behaviour of the Puck class.
 * These tests guard the core game mechanics — friction, ricochet, speed
 * capping,
 * and movement. A regression here means broken in-game physics.
 */
@DisplayName("Puck Physics")
class PuckPhysicsTest {

    private static final double DELTA = 1e-10;

    // ─── Speed Capping ────────────────────────────────────────────

    @Nested
    @DisplayName("Speed capping")
    class SpeedCapping {

        @Test
        @DisplayName("setSpeed caps x and y at MAX_SPEED_CONSTITUENT")
        void setSpeedCaps() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeed(new Speed(999, 999));

            assertEquals(GameConstants.MAX_SPEED_CONSTITUENT, puck.getSpeedX(), DELTA);
            assertEquals(GameConstants.MAX_SPEED_CONSTITUENT, puck.getSpeedY(), DELTA);
        }

        @Test
        @DisplayName("setSpeedXY caps at MAX_SPEED_CONSTITUENT")
        void setSpeedXYCaps() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(999, 999);

            assertEquals(GameConstants.MAX_SPEED_CONSTITUENT, puck.getSpeedX(), DELTA);
            assertEquals(GameConstants.MAX_SPEED_CONSTITUENT, puck.getSpeedY(), DELTA);
        }

        @Test
        @DisplayName("Speed below cap is not modified")
        void speedBelowCapUnchanged() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            double low = GameConstants.MAX_SPEED_CONSTITUENT / 2;
            puck.setSpeedXY(low, low);

            assertEquals(low, puck.getSpeedX(), DELTA);
            assertEquals(low, puck.getSpeedY(), DELTA);
        }
    }

    // ─── Movement ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Movement (onTick)")
    class Movement {

        @Test
        @DisplayName("onTick moves puck by its speed")
        void basicMovement() {
            Position start = new Position(0.5, 0.5);
            Puck puck = Puck.create(start);
            puck.setSpeedXY(0.01, 0.02);

            puck.onTick();

            // onTick applies: move, then friction, then stale check
            // After move, position should be start + speed
            // Friction then modifies speed (but not position this tick)
            assertEquals(0.51, puck.getPosition().x(), 1e-6);
            assertEquals(0.52, puck.getPosition().y(), 1e-6);
        }

        @Test
        @DisplayName("Puck with zero speed at center should remain at center")
        void stationaryPuck() {
            Position start = new Position(0.5, 0.5);
            Puck puck = Puck.create(start);

            puck.onTick();

            assertEquals(start.x(), puck.getPosition().x(), DELTA);
            assertEquals(start.y(), puck.getPosition().y(), DELTA);
        }
    }

    // ─── Friction ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Friction")
    class Friction {

        @Test
        @DisplayName("Speed decreases over multiple ticks due to friction")
        void frictionDecaysSpeed() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(0.01, 0.01);

            double initialSpeedX = puck.getSpeedX();

            // Run several ticks
            for (int i = 0; i < 100; i++) {
                puck.onTick();
            }

            assertTrue(Math.abs(puck.getSpeedX()) < Math.abs(initialSpeedX),
                    "Speed should decay due to friction");
        }

        @Test
        @DisplayName("resetFriction resets the friction counter")
        void resetFriction() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(0.01, 0.01);

            // Apply many ticks to build up friction
            for (int i = 0; i < 50; i++) {
                puck.onTick();
            }
            double speedAfterFriction = Math.abs(puck.getSpeedX());

            // Reset friction and set fresh speed
            puck.resetFriction();
            puck.setSpeedXY(0.01, 0.01);
            puck.onTick();
            double speedAfterReset = Math.abs(puck.getSpeedX());

            assertTrue(speedAfterReset > speedAfterFriction,
                    "After resetFriction, the first tick should apply less friction than 50 accumulated ticks");
        }
    }

    // ─── Ricochet ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Ricochet (reflection off handle)")
    class Ricochet {

        @Test
        @DisplayName("Ricochet off horizontal surface reverses Y direction")
        void horizontalReflection() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(0, 0.01);

            // Reflect off a horizontal normal (unit vector pointing up)
            Vector normal = new Vector(0, 1);
            puck.ricochet(normal);

            assertEquals(0, puck.getSpeedX(), DELTA);
            assertTrue(puck.getSpeedY() < 0, "Y speed should reverse after horizontal ricochet");
        }

        @Test
        @DisplayName("Ricochet off vertical surface reverses X direction")
        void verticalReflection() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(0.01, 0);

            Vector normal = new Vector(1, 0);
            puck.ricochet(normal);

            assertTrue(puck.getSpeedX() < 0, "X speed should reverse after vertical ricochet");
            assertEquals(0, puck.getSpeedY(), DELTA);
        }

        @Test
        @DisplayName("Ricochet off vertical wall preserves speed magnitude")
        void preservesMagnitude() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(0.01, 0);

            // Reflect off a vertical wall (normal along x-axis)
            Vector normal = new Vector(1, 0);
            puck.ricochet(normal);

            // For a pure axis-aligned speed + axis-aligned normal, magnitude should be
            // preserved
            assertEquals(0.01, Math.abs(puck.getSpeedX()), DELTA,
                    "Speed magnitude should be preserved after axis-aligned ricochet");
        }
    }

    // ─── Speed Negation ───────────────────────────────────────────

    @Nested
    @DisplayName("Speed negation")
    class SpeedNegation {

        @Test
        @DisplayName("negateSpeedX flips the X component sign")
        void negateX() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(0.01, 0.02);

            puck.negateSpeedX();

            assertEquals(-0.01, puck.getSpeedX(), DELTA);
            assertEquals(0.02, puck.getSpeedY(), DELTA);
        }

        @Test
        @DisplayName("negateSpeedY flips the Y component sign")
        void negateY() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(0.01, 0.02);

            puck.negateSpeedY();

            assertEquals(0.01, puck.getSpeedX(), DELTA);
            assertEquals(-0.02, puck.getSpeedY(), DELTA);
        }

        @Test
        @DisplayName("Double negation restores original speed")
        void doubleNegation() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(0.01, -0.02);

            puck.negateSpeedX();
            puck.negateSpeedX();

            assertEquals(0.01, puck.getSpeedX(), DELTA);
        }
    }
}

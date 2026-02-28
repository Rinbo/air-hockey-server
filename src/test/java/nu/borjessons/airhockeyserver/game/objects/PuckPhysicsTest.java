package nu.borjessons.airhockeyserver.game.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Speed;

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
    @DisplayName("Speed capping (magnitude-based)")
    class SpeedCapping {

        @Test
        @DisplayName("setSpeed clamps by magnitude, preserving direction")
        void setSpeedClampsByMagnitude() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeed(new Speed(999, 999));

            double mag = Math.sqrt(puck.getSpeedX() * puck.getSpeedX() + puck.getSpeedY() * puck.getSpeedY());
            assertEquals(GameConstants.MAX_SPEED, mag, DELTA, "Magnitude should equal MAX_SPEED");
            // Equal input → equal output components
            assertEquals(puck.getSpeedX(), puck.getSpeedY(), DELTA);
        }

        @Test
        @DisplayName("setSpeedXY clamps by magnitude, preserving direction")
        void setSpeedXYClampsByMagnitude() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(999, 999);

            double mag = Math.sqrt(puck.getSpeedX() * puck.getSpeedX() + puck.getSpeedY() * puck.getSpeedY());
            assertEquals(GameConstants.MAX_SPEED, mag, DELTA, "Magnitude should equal MAX_SPEED");
        }

        @Test
        @DisplayName("Negative speeds are clamped by magnitude symmetrically")
        void negativeSpeedsClamped() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(-999, -999);

            double mag = Math.sqrt(puck.getSpeedX() * puck.getSpeedX() + puck.getSpeedY() * puck.getSpeedY());
            assertEquals(GameConstants.MAX_SPEED, mag, DELTA, "Magnitude should equal MAX_SPEED for negative speeds");
            assertTrue(puck.getSpeedX() < 0, "X should remain negative");
            assertTrue(puck.getSpeedY() < 0, "Y should remain negative");
        }

        @Test
        @DisplayName("Speed below cap is not modified")
        void speedBelowCapUnchanged() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            double low = GameConstants.MAX_SPEED / 4;
            puck.setSpeedXY(low, low);

            assertEquals(low, puck.getSpeedX(), DELTA);
            assertEquals(low, puck.getSpeedY(), DELTA);
        }

        @Test
        @DisplayName("Direction is preserved when clamping")
        void directionPreserved() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(999, 0);

            assertEquals(GameConstants.MAX_SPEED, puck.getSpeedX(), DELTA, "Pure X speed should clamp to MAX_SPEED");
            assertEquals(0, puck.getSpeedY(), DELTA, "Y should remain 0");
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

    // ─── Friction (Constant Damping) ────────────────────────────────

    @Nested
    @DisplayName("Friction (constant damping)")
    class Friction {

        @Test
        @DisplayName("Speed decreases over multiple ticks due to friction")
        void frictionDecaysSpeed() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(0.01, 0.01);

            double initialSpeedX = puck.getSpeedX();

            for (int i = 0; i < 100; i++) {
                puck.onTick();
            }

            assertTrue(Math.abs(puck.getSpeedX()) < Math.abs(initialSpeedX),
                    "Speed should decay due to friction");
        }

        @Test
        @DisplayName("Damping rate is constant (not increasing over time)")
        void constantDampingRate() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(0.01, 0);

            // Measure damping ratio between consecutive ticks at different points
            puck.onTick();
            double speed1 = puck.getSpeedX();
            puck.onTick();
            double speed2 = puck.getSpeedX();
            double ratio1 = speed2 / speed1;

            // Reset and do the same many ticks later
            puck.setSpeedXY(0.01, 0);
            for (int i = 0; i < 200; i++) {
                puck.onTick();
            }
            double speedLate1 = puck.getSpeedX();
            puck.onTick();
            double speedLate2 = puck.getSpeedX();

            // Only check if speed hasn't decayed to threshold
            if (Math.abs(speedLate1) > 1e-5 && Math.abs(speedLate2) > 1e-5) {
                double ratio2 = speedLate2 / speedLate1;
                assertEquals(ratio1, ratio2, 1e-6,
                        "Damping ratio should be constant regardless of elapsed ticks");
            }
        }

        @Test
        @DisplayName("Puck stops completely when speed drops below threshold")
        void puckStopsAtThreshold() {
            Puck puck = Puck.create(new Position(0.5, 0.5));
            puck.setSpeedXY(0.001, 0.001);

            // Run many ticks until the puck should have stopped
            for (int i = 0; i < 10000; i++) {
                puck.onTick();
            }

            assertEquals(0, puck.getSpeedX(), DELTA, "X speed should reach exactly 0");
            assertEquals(0, puck.getSpeedY(), DELTA, "Y speed should reach exactly 0");
        }
    }
}

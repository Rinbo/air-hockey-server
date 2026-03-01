package nu.borjessons.airhockeyserver.game;

import nu.borjessons.airhockeyserver.game.properties.Position;

/**
 * Mutable broadcast state object, pre-allocated to avoid GC pressure in the
 * game loop.
 * Serializes to the same JSON shape as the original record:
 * {"opponent": {"x": ..., "y": ...}, "puck": {"x": ..., "y": ...},
 * "remainingSeconds": ...}
 */
public class BroadcastState {
    /**
     * Collision event flags — sent as a bitmask to the client for sound triggers.
     */
    public static final int NO_EVENT = 0;
    public static final int WALL_HIT = 1;
    public static final int HANDLE_HIT = 2;
    public static final int GOAL = 4;

    private final MutablePosition opponent = new MutablePosition();
    private final MutablePosition puck = new MutablePosition();
    private long remainingSeconds;
    private int collisionEvent;

    public void set(Position opponentPos, Position puckPos, long remainingSeconds, int collisionEvent) {
        this.opponent.set(opponentPos.x(), opponentPos.y());
        this.puck.set(puckPos.x(), puckPos.y());
        this.remainingSeconds = remainingSeconds;
        this.collisionEvent = collisionEvent;
    }

    public void setMirrored(Position opponentPos, Position puckPos, long remainingSeconds, int collisionEvent) {
        this.opponent.set(1 - opponentPos.x(), 1 - opponentPos.y());
        this.puck.set(1 - puckPos.x(), 1 - puckPos.y());
        this.remainingSeconds = remainingSeconds;
        this.collisionEvent = collisionEvent;
    }

    public MutablePosition getOpponent() {
        return opponent;
    }

    public MutablePosition getPuck() {
        return puck;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getCollisionEvent() {
        return collisionEvent;
    }

    /**
     * Simple mutable x/y holder that serializes identically to a Position record.
     */
    public static class MutablePosition {
        private double x;
        private double y;

        public void set(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }
}

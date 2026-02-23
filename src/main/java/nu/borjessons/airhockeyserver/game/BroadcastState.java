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
    private final MutablePosition opponent = new MutablePosition();
    private final MutablePosition puck = new MutablePosition();
    private long remainingSeconds;

    public void set(Position opponentPos, Position puckPos, long remainingSeconds) {
        this.opponent.set(opponentPos.x(), opponentPos.y());
        this.puck.set(puckPos.x(), puckPos.y());
        this.remainingSeconds = remainingSeconds;
    }

    public void setMirrored(Position opponentPos, Position puckPos, long remainingSeconds) {
        this.opponent.set(1 - opponentPos.x(), 1 - opponentPos.y());
        this.puck.set(1 - puckPos.x(), 1 - puckPos.y());
        this.remainingSeconds = remainingSeconds;
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

package se.docksidelabs.airhockeyserver.game;

import se.docksidelabs.airhockeyserver.game.properties.Position;

/**
 * Pre-allocated, mutable broadcast payload — avoids per-tick GC pressure.
 *
 * <p>Each player gets their own instance with coordinates oriented so
 * that "their" handle is always at the bottom of the screen. Player 2's
 * view is mirrored via {@link #setMirrored}.
 */
public class BroadcastState {

  /** Collision event bitmask flags — used by the client for sound triggers. */
  public static final int NO_EVENT   = 0;
  public static final int WALL_HIT   = 1;
  public static final int HANDLE_HIT = 2;
  public static final int GOAL       = 4;

  private final MutablePosition opponent = new MutablePosition();
  private final MutablePosition puck = new MutablePosition();
  private long remainingSeconds;
  private int collisionEvent;

  public void set(Position opponentPosition, Position puckPosition,
                  long remainingSeconds, int collisionEvent) {
    this.opponent.set(opponentPosition.x(), opponentPosition.y());
    this.puck.set(puckPosition.x(), puckPosition.y());
    this.remainingSeconds = remainingSeconds;
    this.collisionEvent = collisionEvent;
  }

  public void setMirrored(Position opponentPosition, Position puckPosition,
                           long remainingSeconds, int collisionEvent) {
    this.opponent.set(1 - opponentPosition.x(), 1 - opponentPosition.y());
    this.puck.set(1 - puckPosition.x(), 1 - puckPosition.y());
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
   * Mutable x/y holder that serializes identically to a {@link Position} record.
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

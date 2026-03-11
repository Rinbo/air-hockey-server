package se.docksidelabs.airhockeyserver.game.objects;

import java.util.Objects;

import se.docksidelabs.airhockeyserver.game.properties.GameConstants;
import se.docksidelabs.airhockeyserver.game.properties.Position;
import se.docksidelabs.airhockeyserver.game.properties.Radius;
import se.docksidelabs.airhockeyserver.game.properties.Speed;

/**
 * The player's handle (mallet). Position updates are clamped to prevent
 * teleportation on fast swipes — this doubles as server-side anti-cheat.
 */
public final class Handle extends Circle {

  private volatile double speedX;
  private volatile double speedY;
  private double previousX;
  private double previousY;

  private Handle(Position position, Radius radius) {
    super(position, radius);
    this.previousX = position.x();
    this.previousY = position.y();
  }

  public static Handle create(Position position) {
    Objects.requireNonNull(position, "position must not be null");
    return new Handle(position, GameConstants.HANDLE_RADIUS);
  }

  public static Handle create(Position position, Radius radius) {
    Objects.requireNonNull(position, "position must not be null");
    Objects.requireNonNull(radius, "radius must not be null");
    return new Handle(position, radius);
  }

  // ── Accessors ────────────────────────────────────────────────────

  public Position getPreviousPosition() {
    return new Position(previousX, previousY);
  }

  public Speed getSpeed() {
    return new Speed(speedX, speedY);
  }

  // ── Position Updates ─────────────────────────────────────────────

  /**
   * Moves the handle toward the target position, clamping displacement
   * to {@link GameConstants#MAX_HANDLE_DISTANCE} per update.
   */
  @Override
  public void setPosition(Position target) {
    Position current = getPosition();
    double deltaX = target.x() - current.x();
    double deltaY = target.y() - current.y();
    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    if (distance > GameConstants.MAX_HANDLE_DISTANCE) {
      double scale = GameConstants.MAX_HANDLE_DISTANCE / distance;
      target = new Position(current.x() + deltaX * scale, current.y() + deltaY * scale);
    }

    super.setPosition(target);
  }

  /**
   * Sets position without velocity clamping — for resets and initialization only.
   */
  public void forcePosition(Position position) {
    super.setPosition(position);
  }

  /**
   * Snapshots the current position as "previous" and computes this
   * frame's velocity. Called once per tick after physics resolution.
   */
  public void updateSpeed() {
    Position position = getPosition();
    speedX = position.x() - previousX;
    speedY = position.y() - previousY;
    previousX = position.x();
    previousY = position.y();
  }
}

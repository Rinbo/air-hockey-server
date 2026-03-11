package se.docksidelabs.airhockeyserver.game.objects;

import java.util.Objects;

import se.docksidelabs.airhockeyserver.game.properties.GameConstants;
import se.docksidelabs.airhockeyserver.game.properties.Position;
import se.docksidelabs.airhockeyserver.game.properties.Radius;
import se.docksidelabs.airhockeyserver.game.properties.Speed;

public final class Puck extends Circle {

  private static final double SPEED_STOP_THRESHOLD = 1e-6;
  private static final double WALL_CONTACT_EPSILON = 1e-9;

  private double speedX;
  private double speedY;

  private Puck(Position position, Radius radius) {
    super(position, radius);
  }

  public static Puck create(Position position) {
    return create(position, GameConstants.PUCK_RADIUS);
  }

  public static Puck create(Position position, Radius radius) {
    Objects.requireNonNull(position, "position must not be null");
    Objects.requireNonNull(radius, "radius must not be null");
    return new Puck(position, radius);
  }

  // ── Accessors ────────────────────────────────────────────────────

  public Speed getSpeed() {
    return new Speed(speedX, speedY);
  }

  public double getSpeedX() {
    return speedX;
  }

  public double getSpeedY() {
    return speedY;
  }

  public void setSpeed(Speed speed) {
    this.speedX = speed.x();
    this.speedY = speed.y();
    clampSpeed();
  }

  public void setSpeedXY(double x, double y) {
    this.speedX = x;
    this.speedY = y;
    clampSpeed();
  }

  // ── Physics Tick ─────────────────────────────────────────────────

  public void onTick() {
    onSubTick(1);
  }

  /**
   * Advances the puck by one sub-step. Movement is divided by {@code steps}
   * and friction is applied as {@code FRICTION^(1/steps)} so the total effect
   * over all sub-steps equals a single full tick.
   */
  public void onSubTick(int steps) {
    Position position = getPosition();
    advancePosition(position, steps);
    applyFriction(steps);
    nudgeAwayFromWallContact(position);
  }

  // ── Internal Physics ─────────────────────────────────────────────

  private void advancePosition(Position position, int steps) {
    setPosition(new Position(
        position.x() + speedX / steps,
        position.y() + speedY / steps));
  }

  private void applyFriction(int steps) {
    double damping = (steps == 1)
        ? GameConstants.FRICTION_DAMPING
        : Math.pow(GameConstants.FRICTION_DAMPING, 1.0 / steps);

    speedX *= damping;
    speedY *= damping;

    if (Math.abs(speedX) < SPEED_STOP_THRESHOLD) speedX = 0;
    if (Math.abs(speedY) < SPEED_STOP_THRESHOLD) speedY = 0;
  }

  /**
   * If the puck has come to rest (speed == 0) exactly on a wall boundary,
   * give it a tiny nudge inward so it doesn't stick. Only replaces a
   * speed component when it's already zero.
   */
  private void nudgeAwayFromWallContact(Position position) {
    Radius radius = getRadius();
    double newSpeedX = (speedX == 0) ? wallContactNudgeX(position.x(), radius.x()) : speedX;
    double newSpeedY = (speedY == 0) ? wallContactNudgeY(position.y(), radius.y()) : speedY;

    if (newSpeedX != speedX || newSpeedY != speedY) {
      speedX = newSpeedX;
      speedY = newSpeedY;
    }
  }

  private static double wallContactNudgeX(double x, double radiusX) {
    if (Math.abs(x - radiusX) < WALL_CONTACT_EPSILON) return 1;
    if (Math.abs(x - (1 - radiusX)) < WALL_CONTACT_EPSILON) return -1;
    return 0;
  }

  private static double wallContactNudgeY(double y, double radiusY) {
    if (Math.abs(y - radiusY) < WALL_CONTACT_EPSILON) return 1;
    if (Math.abs(y - (1 - radiusY)) < WALL_CONTACT_EPSILON) return -1;
    return 0;
  }

  private void clampSpeed() {
    double magnitude = Math.sqrt(speedX * speedX + speedY * speedY);
    if (magnitude > GameConstants.MAX_SPEED) {
      double scale = GameConstants.MAX_SPEED / magnitude;
      speedX *= scale;
      speedY *= scale;
    }
  }
}

package nu.borjessons.airhockeyserver.game.objects;

import java.util.Objects;

import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Radius;
import nu.borjessons.airhockeyserver.game.properties.Speed;

public final class Puck extends Circle {
  private double speedX;
  private double speedY;

  private Puck(Position position, Radius radius) {
    super(position, radius);
  }

  public static Puck copyOf(Puck puck) {
    return Puck.create(puck.getPosition(), puck.getRadius());
  }

  public static Puck create(Position position) {
    return Puck.create(position, GameConstants.PUCK_RADIUS);
  }

  public static Puck create(Position position, Radius radius) {
    Objects.requireNonNull(position, "position must not be null");
    Objects.requireNonNull(radius, "radius must not be null");

    return new Puck(position, radius);
  }

  private static final double EPSILON = 1e-9;

  private static double getXRecoverySpeed(double xCoordinate, double xRadius) {
    if (Math.abs(xCoordinate - xRadius) < EPSILON)
      return 1;
    if (Math.abs(xCoordinate - (1 - xRadius)) < EPSILON)
      return -1;
    return 0;
  }

  private static double getYRecoverySpeed(double yCoordinate, double yRadius) {
    if (Math.abs(yCoordinate - yRadius) < EPSILON)
      return 1;
    if (Math.abs(yCoordinate - (1 - yRadius)) < EPSILON)
      return -1;
    return 0;
  }

  public Speed getSpeed() {
    return new Speed(speedX, speedY);
  }

  public double getSpeedX() {
    return speedX;
  }

  public double getSpeedY() {
    return speedY;
  }

  public void onTick() {
    Position position = super.getPosition();
    Radius radius = getRadius();

    movePuck(position);
    handleFriction();
    handleStalePuck(position, radius);
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

  private void clampSpeed() {
    double mag = Math.sqrt(speedX * speedX + speedY * speedY);
    if (mag > GameConstants.MAX_SPEED) {
      double scale = GameConstants.MAX_SPEED / mag;
      speedX *= scale;
      speedY *= scale;
    }
  }

  private static final double SPEED_STOP_THRESHOLD = 1e-6;

  private void handleFriction() {
    this.speedX *= GameConstants.FRICTION_DAMPING;
    this.speedY *= GameConstants.FRICTION_DAMPING;
    if (Math.abs(speedX) < SPEED_STOP_THRESHOLD)
      speedX = 0;
    if (Math.abs(speedY) < SPEED_STOP_THRESHOLD)
      speedY = 0;
  }

  private void handleStalePuck(Position position, Radius radius) {
    double newX = speedX == 0 ? getXRecoverySpeed(position.x(), radius.x()) : speedX;
    double newY = speedY == 0 ? getYRecoverySpeed(position.y(), radius.y()) : speedY;
    if (newX != speedX || newY != speedY) {
      this.speedX = newX;
      this.speedY = newY;
    }
  }

  private void movePuck(Position position) {
    setPosition(new Position(position.x() + speedX, position.y() + speedY));
  }
}

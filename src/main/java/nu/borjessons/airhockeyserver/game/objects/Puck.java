package nu.borjessons.airhockeyserver.game.objects;

import java.util.Objects;

import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Radius;
import nu.borjessons.airhockeyserver.game.properties.Speed;
import nu.borjessons.airhockeyserver.game.properties.Vector;

public final class Puck extends Circle {
  private long friction;
  private double speedX;
  private double speedY;

  private Puck(Position position, Radius radius) {
    super(position, radius);

    friction = 0;
    speedX = 0;
    speedY = 0;
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

  private static double dotProduct(double sx, double sy, Vector vector) {
    return sx * vector.x() + sy * vector.y();
  }

  private static double getXRecoverySpeed(double xCoordinate, double xRadius) {
    if (xCoordinate == 0 + xRadius)
      return 1;
    if (xCoordinate == 1 - xRadius)
      return -1;
    return 0;
  }

  private static double getYRecoverySpeed(double yCoordinate, double yRadius) {
    if (yCoordinate == 0 + yRadius)
      return 1;
    if (yCoordinate == 1 - yRadius)
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

  public void offsetCollisionWith(Handle handle, double angle) {
    Position handleRadiusEdgePos = handle.getRadiusEdgePosition(angle + Math.PI);
    Position puckRadiusEdgePosition = super.getRadiusEdgePosition(angle);

    Position position = getPosition();
    double xOffset = handleRadiusEdgePos.x() - puckRadiusEdgePosition.x();
    double yOffset = handleRadiusEdgePos.y() - puckRadiusEdgePosition.y();
    setPosition(new Position(position.x() + xOffset, position.y() + yOffset));
  }

  public void onTick() {
    Position position = super.getPosition();
    Radius radius = getRadius();

    movePuck(position);
    handleFriction();
    handleStalePuck(position, radius);
  }

  public void resetFriction() {
    this.friction = 0;
  }

  public void ricochet(Vector vector) {
    double dp = dotProduct(speedX, speedY, vector);
    double dpVec = dotProduct(vector.x(), vector.y(), vector);
    double scalar = dp / dpVec * -1;
    speedX = vector.x() * scalar;
    speedY = vector.y() * scalar;
  }

  public void setSpeed(Speed speed) {
    this.speedX = Math.min(GameConstants.MAX_SPEED_CONSTITUENT, speed.x());
    this.speedY = Math.min(GameConstants.MAX_SPEED_CONSTITUENT, speed.y());
  }

  public void setSpeedXY(double x, double y) {
    this.speedX = Math.min(GameConstants.MAX_SPEED_CONSTITUENT, x);
    this.speedY = Math.min(GameConstants.MAX_SPEED_CONSTITUENT, y);
  }

  public void negateSpeedX() {
    this.speedX = -this.speedX;
  }

  public void negateSpeedY() {
    this.speedY = -this.speedY;
  }

  private double getFrictionCoefficient() {
    return 1 / ((GameConstants.FRICTION_MODIFIER + friction) / GameConstants.FRICTION_MODIFIER);
  }

  private void handleFriction() {
    double frictionCoefficient = getFrictionCoefficient();
    this.speedX *= frictionCoefficient;
    this.speedY *= frictionCoefficient;
    this.friction++;
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

package nu.borjessons.airhockeyserver.game.properties;

/**
 * If  angle could be expressed so that using the angle to get the hypotenuse
 */
public record Vector(double x, double y) {
  public static Vector from(Position position1, Position position2) {
    return new Vector(position1.x() - position2.x(), position1.y() - position2.y());
  }

  public double angle() {
    return Math.atan(y / x); // TODO Remember the annoyance with the board aspect ratio
  }

  public double angle(double aspectRatio) {
    return Math.atan((y / aspectRatio) / x);
  }
}

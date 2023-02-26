package nu.borjessons.airhockeyserver.game.properties;

/**
 * Since the position is expressed a percentage of board width and height,
 * the angle between them must be normalized with respect to one axis. In this
 * implementation the width is chosen.
 */
public record Vector(double x, double y) {
  public static Vector from(Position position1, Position position2) {
    return new Vector(position1.x() - position2.x(), position1.y() - position2.y());
  }

  public double angle() {
    return angle(GameConstants.BOARD_ASPECT_RATIO);
  }

  public double angle(double aspectRatio) {
    return Math.atan2((y / aspectRatio), x);
  }
}

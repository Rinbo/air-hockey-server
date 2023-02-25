package nu.borjessons.airhockeyserver.game.properties;

/**
 * The radius as expressed as a percentage of board width and height.
 * The constituent parts x and y are not independent.
 * h = board height
 * w = board width
 * ar = aspect ratio (w / h)
 * c = radius absolute value
 * <p>
 * x = c / w
 * y = c / h
 * c = y * h
 * c = x * w
 * x * w = y * h
 * x = y * (h / w) => x = y / ar
 * y = x * (w / h) => y = x * ar
 */
public record Radius(double x, double y) {
  public Radius getAngledProjection(double angle) {
    return new Radius(x * Math.cos(angle), y * Math.sin(angle));
  }
}

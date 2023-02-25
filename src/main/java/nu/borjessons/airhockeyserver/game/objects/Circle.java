package nu.borjessons.airhockeyserver.game.objects;

import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Radius;

public abstract class Circle {
  private Position position;
  private final Radius radius;

  protected Circle(Position position, Radius radius) {
    this.position = position;
    this.radius = radius;
  }

  public Position getPosition() {
    return position;
  }

  public Radius getRadius() {
    return radius;
  }

  public Position getRadiusEdgePosition(double angle) {
    Radius projection = getRadius().getAngledProjection(angle);
    return new Position(position.x() + projection.x(), position.y() + projection.y());
  }

  public void setPosition(Position position) {
    this.position = position;
  }
}

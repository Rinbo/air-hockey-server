package se.docksidelabs.airhockeyserver.game.objects;

import se.docksidelabs.airhockeyserver.game.properties.Position;
import se.docksidelabs.airhockeyserver.game.properties.Radius;

/**
 * Base class for circular game objects (puck and handle).
 * Position is volatile to support cross-thread reads from the broadcast loop.
 */
public abstract class Circle {

  private volatile Position position;
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

  public void setPosition(Position position) {
    this.position = position;
  }
}

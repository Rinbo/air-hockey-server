package nu.borjessons.airhockeyserver.engine;

import java.util.Objects;

import nu.borjessons.airhockeyserver.engine.properties.Position;
import nu.borjessons.airhockeyserver.engine.properties.Speed;

final class Puck extends GameObject {
  private Puck(Position position) {
    super(position);
  }

  static Puck create(Position position) {
    Objects.requireNonNull(position, "position must not be null");

    return new Puck(position);
  }

  void move() {
    Position position = super.getPosition();
    Speed speed = super.getSpeed();

    double x = Math.max(0, position.x() + speed.x() - GameConstants.PUCK_RADIUS.x());
    double y = Math.max(0, position.y() + speed.y() - GameConstants.PUCK_RADIUS.y());
    setPosition(new Position(Math.min(1, x + GameConstants.PUCK_RADIUS.x()), Math.min(1, y + GameConstants.PUCK_RADIUS.y())));
  }

  void ricochet() {
    Speed speed = super.getSpeed();
    super.setSpeed(new Speed(speed.x() * -1, speed.y() * -1));
  }
}

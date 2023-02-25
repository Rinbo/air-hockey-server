package nu.borjessons.airhockeyserver.game.objects;

import java.util.Objects;

import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Radius;
import nu.borjessons.airhockeyserver.game.properties.Speed;

public final class Puck extends Circle {
  private Speed speed;

  private Puck(Position position, Radius radius) {
    super(position, radius);

    speed = GameConstants.ZERO_SPEED;
  }

  public static Puck create(Position position) {
    return Puck.create(position, GameConstants.PUCK_RADIUS);
  }

  public static Puck create(Position position, Radius radius) {
    Objects.requireNonNull(position, "position must not be null");

    return new Puck(position, radius);
  }

  public Speed getSpeed() {
    return speed;
  }

  public void move() {
    Position position = super.getPosition();

    double x = Math.max(0, position.x() + speed.x() - GameConstants.PUCK_RADIUS.x());
    double y = Math.max(0, position.y() + speed.y() - GameConstants.PUCK_RADIUS.y());
    setPosition(new Position(Math.min(1, x + GameConstants.PUCK_RADIUS.x()), Math.min(1, y + GameConstants.PUCK_RADIUS.y())));
  }

  public void ricochet() {
    speed = new Speed(speed.x() * -1, speed.y() * -1);
  }

  public void setSpeed(Speed speed) {
    this.speed = speed;
  }
}

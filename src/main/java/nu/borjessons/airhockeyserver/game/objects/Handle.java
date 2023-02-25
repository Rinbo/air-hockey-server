package nu.borjessons.airhockeyserver.game.objects;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Speed;

public final class Handle extends Circle {
  private Position previousPosition;
  private final AtomicReference<Speed> speedReference;

  private Handle(Position position) {
    super(position, GameConstants.HANDLE_RADIUS);

    this.previousPosition = position;
    this.speedReference = new AtomicReference<>(GameConstants.ZERO_SPEED);
  }

  public static Handle create(Position position) {
    Objects.requireNonNull(position, "position must not be null");

    return new Handle(position);
  }

  public Speed getSpeed() {
    return speedReference.get();
  }

  public void setSpeed(Speed speed) {
    speedReference.set(speed);
  }

  public void updateSpeed() {
    Position position = super.getPosition();
    setSpeed(new Speed(position.x() - previousPosition.x(), position.y() - previousPosition.y()));
    previousPosition = position;
  }
}

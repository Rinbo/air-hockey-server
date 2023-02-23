package nu.borjessons.airhockeyserver.game;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Speed;

final class Handle {
  private Position position;
  private Position previousPosition;
  private AtomicReference<Speed> speedReference;

  private Handle(Position position) {
    this.position = position;
    this.previousPosition = position;
    this.speedReference = new AtomicReference<>(GameConstants.ZERO_SPEED);
  }

  static Handle create(Position position) {
    Objects.requireNonNull(position, "position must not be null");

    return new Handle(position);
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  Position getPosition() {
    return position;
  }

  Speed getSpeed() {
    return speedReference.get();
  }

  void setSpeed(Speed speed) {
    speedReference.set(speed);
  }

  void updateSpeed() {
    setSpeed(new Speed(position.x() - previousPosition.x(), position.y() - previousPosition.y()));
    previousPosition = position;
  }
}

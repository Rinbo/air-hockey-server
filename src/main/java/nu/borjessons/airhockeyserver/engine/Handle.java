package nu.borjessons.airhockeyserver.engine;

import java.util.Objects;

final class Handle extends GameObject {
  private Position previousPosition;

  private Handle(Position position) {
    super(position);
    this.previousPosition = position;
  }

  static Handle create(Position position) {
    Objects.requireNonNull(position, "position must not be null");

    return new Handle(position);
  }

  void updateSpeed() {
    setSpeed(new Speed(super.getPosition().x() - previousPosition.x(), super.getPosition().y() - previousPosition.y()));
    previousPosition = super.getPosition();
  }
}

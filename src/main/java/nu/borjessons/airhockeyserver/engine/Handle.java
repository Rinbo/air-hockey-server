package nu.borjessons.airhockeyserver.engine;

import java.util.Objects;

final class Handle extends GO {
  private Position previousPosition;

  private Handle(Position position) {
    super(position);
    this.previousPosition = position;
  }

  static Handle create(Position position) {
    Objects.requireNonNull(position, "position must not be null");

    return new Handle(position);
  }

  Position getPreviousPosition() {
    return previousPosition;
  }

  void setPreviousPosition(Position previousPosition) {
    this.previousPosition = previousPosition;
  }

  void updateSpeed() {
    setSpeed(new Speed(super.getPosition().x() - previousPosition.x(), super.getPosition().y() - previousPosition.y()));
  }
}

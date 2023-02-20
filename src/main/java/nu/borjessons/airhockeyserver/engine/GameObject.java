package nu.borjessons.airhockeyserver.engine;

abstract class GameObject {
  private Position position;
  private Speed speed;

  GameObject(Position position) {
    this.position = position;
    this.speed = GameConstants.ZERO_SPEED;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  Position getPosition() {
    return position;
  }

  Speed getSpeed() {
    return speed;
  }

  void setSpeed(Speed speed) {
    this.speed = speed;
  }
}

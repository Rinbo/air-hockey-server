package nu.borjessons.airhockeyserver.engine;

import java.util.Objects;

final class Puck extends GO {
  private Puck(Position position) {
    super(position);
  }

  static Puck create(Position position) {
    Objects.requireNonNull(position, "position must not be null");

    return new Puck(position);
  }
}

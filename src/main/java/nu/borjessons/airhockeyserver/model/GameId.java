package nu.borjessons.airhockeyserver.model;

import java.util.Objects;

public record GameId(String string) {
  public GameId {
    Objects.requireNonNull(string, "string must not be null");
  }

  @Override
  public String toString() {
    return string;
  }
}

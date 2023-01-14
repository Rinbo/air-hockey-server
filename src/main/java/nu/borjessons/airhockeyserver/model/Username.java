package nu.borjessons.airhockeyserver.model;

import java.util.Objects;

public record Username(String string) {
  public Username {
    Objects.requireNonNull(string, "string must not be null");
  }

  @Override
  public String toString() {
    return string;
  }
}

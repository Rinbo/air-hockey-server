package nu.borjessons.airhockeyserver.model;

import java.util.Locale;
import java.util.Objects;

public record Username(String string) {
  public Username {
    Objects.requireNonNull(string, "string must not be null");
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;

    Username other = (Username) object;

    return string.equalsIgnoreCase(other.string);
  }

  @Override
  public int hashCode() {
    return string.toLowerCase(Locale.ROOT).hashCode();
  }

  @Override
  public String toString() {
    return string;
  }
}

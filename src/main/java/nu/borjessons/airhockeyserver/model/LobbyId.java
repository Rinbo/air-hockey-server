package nu.borjessons.airhockeyserver.model;

import java.util.Objects;

public class LobbyId {
  public static LobbyId ofString(String string) {
    Objects.requireNonNull(string, "string must not be null");

    return new LobbyId(string);
  }

  private final String string;

  private LobbyId(String string) {
    this.string = string;
  }

  @Override
  public int hashCode() {
    return Objects.hash(string);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;
    LobbyId other = (LobbyId) object;
    return string.equals(other.string);
  }

  @Override
  public String toString() {
    return string;
  }
}

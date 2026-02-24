package nu.borjessons.airhockeyserver.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Username {
  private Instant instant;
  private final String string;

  @JsonCreator
  public Username(String string) {
    Objects.requireNonNull(string, "string must not be null");

    this.instant = Instant.now();
    this.string = string;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || getClass() != object.getClass())
      return false;

    Username other = (Username) object;

    return string.equalsIgnoreCase(other.string);
  }

  public Instant getInstant() {
    return instant;
  }

  public String getTrimmed() {
    return string.split("\\$")[0];
  }

  @Override
  public int hashCode() {
    return string.toLowerCase(Locale.ROOT).hashCode();
  }

  public void setInstant(Instant instant) {
    this.instant = instant;
  }

  @JsonValue
  @Override
  public String toString() {
    return string;
  }
}

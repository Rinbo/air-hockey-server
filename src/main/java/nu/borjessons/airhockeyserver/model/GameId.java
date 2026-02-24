package nu.borjessons.airhockeyserver.model;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record GameId(String string) {
  @JsonCreator
  public GameId {
    Objects.requireNonNull(string, "string must not be null");
  }

  @JsonValue
  @Override
  @NonNull
  public String toString() {
    return string;
  }
}

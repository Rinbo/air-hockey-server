package nu.borjessons.airhockeyserver.model;

import java.util.Objects;

public record UserMessage(Username username, String message) {
  public UserMessage {
    Objects.requireNonNull(username, "username must not be null");
    Objects.requireNonNull(message, "message must not be null");
  }
}

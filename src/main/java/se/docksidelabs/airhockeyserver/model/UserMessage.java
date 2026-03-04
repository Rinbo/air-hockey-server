package se.docksidelabs.airhockeyserver.model;

import java.util.Objects;

public record UserMessage(Username username, String message, String gatewayUserId) {
  public UserMessage {
    Objects.requireNonNull(username, "username must not be null");
    Objects.requireNonNull(message, "message must not be null");
  }

  /** Convenience constructor for messages without gatewayUserId (e.g. chat). */
  public UserMessage(Username username, String message) {
    this(username, message, null);
  }
}

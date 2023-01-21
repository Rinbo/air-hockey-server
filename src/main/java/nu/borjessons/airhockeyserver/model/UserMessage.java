package nu.borjessons.airhockeyserver.model;

import java.time.ZonedDateTime;

public record UserMessage(Username username, String message, ZonedDateTime datetime) {
}

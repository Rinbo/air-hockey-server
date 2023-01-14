package nu.borjessons.airhockeyserver.model;

import java.time.ZonedDateTime;

public record ChatMessage(Username username, String message, ZonedDateTime datetime) {
}

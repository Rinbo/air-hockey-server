package nu.borjessons.airhockeyserver.game;

import nu.borjessons.airhockeyserver.game.properties.Position;

public record BroadcastState(Position opponent, Position puck) {
}

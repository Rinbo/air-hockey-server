package nu.borjessons.airhockeyserver.engine;

public record GameState(GameObjectState puck, GameObjectState playerOne, GameObjectState playerTwo) {
}

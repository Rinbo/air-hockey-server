package nu.borjessons.airhockeyserver.utils;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.UserMessage;
import nu.borjessons.airhockeyserver.model.Username;

public final class TopicUtils {
  public static final String GAMES_TOPIC = "/topic/games";
  public static final Username GAME_BOT = new Username("Game Bot");

  private TopicUtils() {
    throw new IllegalStateException();
  }

  public static UserMessage createBotMessage(String message) {
    return new UserMessage(GAME_BOT, message);
  }

  public static String createChatTopic(GameId gameId) {
    return createChatTopic(gameId.toString());
  }

  public static String createChatTopic(String gameId) {
    return String.format("/topic/game/%s/chat", gameId);
  }

  public static String createGameStateTopic(GameId gameId) {
    return createGameStateTopic(gameId.toString());
  }

  public static String createGameStateTopic(String gameId) {
    return String.format("/topic/game/%s/game-state", gameId);
  }

  public static String createPlayerTopic(GameId gameId) {
    return createPlayerTopic(gameId.toString());
  }

  public static String createPlayerTopic(String gameId) {
    return String.format("/topic/game/%s/players", gameId);
  }

  public static String createUserTopic(Username username) {
    return String.format("/topic/game/%s/game-state", username);
  }
}

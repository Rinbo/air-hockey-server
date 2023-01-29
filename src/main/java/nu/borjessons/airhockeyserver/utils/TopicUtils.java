package nu.borjessons.airhockeyserver.utils;

import nu.borjessons.airhockeyserver.model.UserMessage;
import nu.borjessons.airhockeyserver.model.Username;

public final class TopicUtils {
  public static final Username GAME_BOT = new Username("Game Bot");

  public static String createChatTopic(String gameId) {
    return String.format("/topic/game/%s/chat", gameId);
  }

  public static UserMessage createConnectMessage(UserMessage userMessage) {
    return new UserMessage(GAME_BOT, userMessage.username() + " joined", userMessage.datetime());
  }

  public static UserMessage createDisconnectMessage(UserMessage userMessage) {
    return new UserMessage(GAME_BOT, userMessage.username() + " left", userMessage.datetime());
  }

  public static String createGameStateTopic(String gameId) {
    return String.format("/topic/game/%s/game-state", gameId);
  }

  public static String createPlayerTopic(String gameId) {
    return String.format("/topic/game/%s/players", gameId);
  }

  private TopicUtils() {
    throw new IllegalStateException();
  }
}

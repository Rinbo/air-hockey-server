package nu.borjessons.airhockeyserver.controller.ws;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Notification;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.UserMessage;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.service.api.GameService;

@Controller
public class GameController {
  private static final Logger logger = LoggerFactory.getLogger(GameController.class);

  private static String createChatTopic(String gameId) {
    return String.format("/topic/game/%s/chat", gameId);
  }

  private static UserMessage createConnectMessage(UserMessage userMessage) {
    return new UserMessage(new Username(Agency.GAME_BOT.toString()), userMessage.username() + " joined", userMessage.datetime());
  }

  private static UserMessage createDisconnectMessage(UserMessage userMessage) {
    return new UserMessage(new Username(Agency.GAME_BOT.toString()), userMessage.username() + " left", userMessage.datetime());
  }

  private static String createNotificationTopic(String gameId) {
    return String.format("/topic/game/%s/notification", gameId);
  }

  private static String createPlayerTopic(String gameId) {
    return String.format("/topic/game/%s/players", gameId);
  }

  private static String getAttribute(SimpMessageHeaderAccessor header, String key) {
    return getMap(header).map(map -> (String) map.get(key)).orElse("");
  }

  private static Optional<Map<String, Object>> getMap(SimpMessageHeaderAccessor header) {
    return Optional.ofNullable(header.getSessionAttributes());
  }

  private static void setAttribute(SimpMessageHeaderAccessor header, String key, String value) {
    getMap(header).ifPresent(map -> map.put(key, value));
  }

  private final GameService gameService;
  private final SimpMessagingTemplate messagingTemplate;

  public GameController(SimpMessagingTemplate messagingTemplate, GameService gameService) {
    this.messagingTemplate = messagingTemplate;
    this.gameService = gameService;
  }

  // TODO add validation so that only the two players in the gameStore are allowed to send here
  @MessageMapping("/game/{id}/chat")
  public void handleChat(@DestinationVariable String id, @Payload UserMessage userMessage, SimpMessageHeaderAccessor header) {
    logger.info("{} in game-{} sent a message", getAttribute(header, "username"), getAttribute(header, "gameId"));

    messagingTemplate.convertAndSend(createChatTopic(id), userMessage);
  }

  @MessageMapping("/game/{id}/connect")
  public void handleConnect(@DestinationVariable String id, @Payload UserMessage userMessage, SimpMessageHeaderAccessor header) {
    Username username = userMessage.username();
    GameId gameId = new GameId(id);

    if (gameService.addUserToGame(gameId, username)) {
      setAttribute(header, "username", username.toString());
      setAttribute(header, "gameId", id);

      messagingTemplate.convertAndSend(createChatTopic(id), createConnectMessage(userMessage));
    }

    messagingTemplate.convertAndSend(createPlayerTopic(id), gameService.getPlayers(gameId));
  }

  @MessageMapping("/game/{id}/disconnect")
  public void handleDisconnect(@DestinationVariable String id, @Payload UserMessage userMessage) {
    GameId gameId = new GameId(id);

    logger.info("disconnect event {}", userMessage);

    gameService.getPlayer(gameId, userMessage.username())
        .ifPresentOrElse(player -> handleUserDisconnect(id, userMessage, gameId, player),
            () -> logger.debug("rogue player disconnected from game {}", gameId));
  }

  @MessageMapping("/send-message")
  @SendTo("/topic/public")
  public UserMessage sendMessage(UserMessage userMessage) {
    logger.info("Received message: {}", userMessage);
    return userMessage;
  }

  private void handleUserDisconnect(String id, UserMessage userMessage, GameId gameId, Player player) {
    switch (player.getAgency()) {
      case PLAYER_1 -> {
        messagingTemplate.convertAndSend(createNotificationTopic(id), Notification.CREATOR_DISCONNECT);
        gameService.deleteGame(gameId);
      }
      case PLAYER_2 -> {
        gameService.removeUser(gameId, userMessage.username());
        messagingTemplate.convertAndSend(createPlayerTopic(id), gameService.getPlayers(gameId));
        messagingTemplate.convertAndSend(createChatTopic(id), createDisconnectMessage(userMessage));
      }
      case GAME_BOT -> logger.warn("game bot located in userStore");
    }
  }
}

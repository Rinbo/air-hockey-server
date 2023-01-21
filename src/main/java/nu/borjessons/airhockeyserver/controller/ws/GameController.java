package nu.borjessons.airhockeyserver.controller.ws;

import java.util.Arrays;
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

import nu.borjessons.airhockeyserver.model.Agent;
import nu.borjessons.airhockeyserver.model.ChatMessage;
import nu.borjessons.airhockeyserver.model.LobbyId;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.service.api.GameService;

@Controller
public class GameController {
  private static final Logger logger = LoggerFactory.getLogger(GameController.class);

  private static String createChatTopic(String lobbyId) {
    return String.format("/topic/lobby/%s/chat", lobbyId);
  }

  private static String createNotificationTopic(String lobbyId) {
    return String.format("/topic/lobby/%s/notification", lobbyId);
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

  @MessageMapping("/lobby/{lobbyId}/chat")
  public void handleChat(@DestinationVariable String lobbyId, @Payload ChatMessage chatMessage, SimpMessageHeaderAccessor header) {
    logger.info("{} in lobby-{} sent a message", getAttribute(header, "username"), getAttribute(header, "lobbyId"));

    messagingTemplate.convertAndSend(createChatTopic(lobbyId), chatMessage);
  }

  @MessageMapping("/lobby/{lobbyId}/connect")
  public void handleConnect(@DestinationVariable String lobbyId, @Payload ChatMessage chatMessage, SimpMessageHeaderAccessor header) {
    Username username = chatMessage.username();

    if (gameService.addUserToLobby(LobbyId.ofString(lobbyId), username)) {
      setAttribute(header, "username", username.toString());
      setAttribute(header, "lobbyId", lobbyId);

      logger.info("current players in this game {}", Arrays.toString(gameService.getPlayers(LobbyId.ofString(lobbyId)).toArray()));
      messagingTemplate.convertAndSend(createChatTopic(lobbyId),
          new ChatMessage(new Username(Agent.GAME_ADMIN.toString()), username + " joined", chatMessage.datetime()));
    }
  }

  @MessageMapping("/send-message")
  @SendTo("/topic/public")
  public ChatMessage sendMessage(ChatMessage chatMessage) {
    logger.info("Received message: {}", chatMessage);
    return chatMessage;
  }
}

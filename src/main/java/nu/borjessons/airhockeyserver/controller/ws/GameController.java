package nu.borjessons.airhockeyserver.controller.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import jakarta.servlet.http.HttpSession;
import nu.borjessons.airhockeyserver.model.Agent;
import nu.borjessons.airhockeyserver.model.ChatMessage;
import nu.borjessons.airhockeyserver.model.LobbyId;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.service.api.GameService;

@Controller
public class GameController {
  private static final Logger logger = LoggerFactory.getLogger(GameController.class);
  private final GameService gameService;
  private final SimpMessagingTemplate messagingTemplate;

  public GameController(SimpMessagingTemplate messagingTemplate, GameService gameService) {
    this.messagingTemplate = messagingTemplate;
    this.gameService = gameService;
  }

  @MessageMapping("/chat/{roomId}")
  public void handleChat(@DestinationVariable String roomId, @Payload ChatMessage chatMessage) {
    messagingTemplate.convertAndSend("/topic/room-" + roomId, chatMessage);
  }

  @MessageMapping("/chat/connect/{lobbyId}")
  public void handleConnect(@DestinationVariable String lobbyId, @Payload ChatMessage chatMessage, HttpSession session) {
    Username username = chatMessage.username();
    gameService.addUserToLobby(LobbyId.ofString(lobbyId), username);

    session.setAttribute("username", username.toString());
    session.setAttribute("roomId", lobbyId);

    messagingTemplate.convertAndSend("/topic/room-" + lobbyId,
        new ChatMessage(new Username(Agent.GAME_ADMIN.toString()), username + "joined", chatMessage.datetime()));
  }

  @MessageMapping("/send-message")
  @SendTo("/topic/public")
  public ChatMessage sendMessage(ChatMessage chatMessage) {
    logger.info("Received message: {}", chatMessage);
    return chatMessage;
  }
}

package nu.borjessons.airhockeyserver.controller.ws;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Notification;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.UserMessage;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.service.api.CountdownService;
import nu.borjessons.airhockeyserver.service.api.GameService;
import nu.borjessons.airhockeyserver.utils.TopicUtils;

@Controller
public class GameController {
  private static final String GAME_ID_HEADER = "gameId";
  private static final String USERNAME_HEADER = "username";
  private static final Logger logger = LoggerFactory.getLogger(GameController.class);

  private static UserMessage createBotMessage(String message) {
    return new UserMessage(TopicUtils.GAME_BOT, message);
  }

  private static String format(String message, Object... args) {
    return String.format(Locale.ROOT, message, args);
  }

  private static String getAttribute(SimpMessageHeaderAccessor header, String key) {
    return getMap(header).map(map -> (String) map.get(key)).orElse("");
  }

  private static GameId getGameId(SimpMessageHeaderAccessor header) {
    return new GameId(getAttribute(header, GAME_ID_HEADER));
  }

  private static Optional<Map<String, Object>> getMap(SimpMessageHeaderAccessor header) {
    return Optional.ofNullable(header.getSessionAttributes());
  }

  private static Username getUserName(SimpMessageHeaderAccessor header) {
    return new Username(getAttribute(header, USERNAME_HEADER));
  }

  private static void setAttribute(SimpMessageHeaderAccessor header, String key, String value) {
    getMap(header).ifPresent(map -> map.put(key, value));
  }

  private final CountdownService countdownService;
  private final GameService gameService;
  private final SimpMessagingTemplate messagingTemplate;

  public GameController(CountdownService countdownService, SimpMessagingTemplate messagingTemplate, GameService gameService) {
    this.countdownService = countdownService;
    this.messagingTemplate = messagingTemplate;
    this.gameService = gameService;
  }

  // TODO maybe all cases where I use the Username, I should grab it from the header instead
  // TODO add validation so that only the two players in the gameStore are allowed to send here
  @MessageMapping("/game/{id}/chat")
  public void handleChat(@DestinationVariable String id, @Payload UserMessage userMessage, SimpMessageHeaderAccessor header) {
    logger.info("{} in game-{} sent a message", getAttribute(header, USERNAME_HEADER), getAttribute(header, GAME_ID_HEADER));
    gameService.getGameStore(new GameId(id)).ifPresent(gameStore -> logger.info("gameStore state: {}", gameStore));

    messagingTemplate.convertAndSend(TopicUtils.createChatTopic(id), userMessage);
  }

  @MessageMapping("/game/{id}/connect")
  public void handleConnect(@DestinationVariable String id, @Payload UserMessage userMessage, SimpMessageHeaderAccessor header) {
    Username username = userMessage.username();
    GameId gameId = new GameId(id);

    logger.info("{} connected", username);

    setAttribute(header, USERNAME_HEADER, username.toString());
    setAttribute(header, GAME_ID_HEADER, id);

    if (gameService.addUserToGame(gameId, username)) {
      logger.info("{} added to gameStore", username);
      messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId), createBotMessage(format("%s joined", username)));
    }

    messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameId), gameService.getPlayers(gameId));
  }

  @MessageMapping("/game/{id}/disconnect")
  public void handleDisconnect(@DestinationVariable String id, @Payload UserMessage userMessage) {
    GameId gameId = new GameId(id);

    logger.info("disconnect event {}", userMessage);

    countdownService.cancelTimer(gameId);
    gameService.getPlayer(gameId, userMessage.username())
        .ifPresentOrElse(player -> handleUserDisconnect(gameId, player),
            () -> logger.debug("rogue player disconnected from game {}", gameId));
  }

  @MessageMapping("/game/{id}/toggle-ready")
  public void toggleReady(@DestinationVariable String id, SimpMessageHeaderAccessor header) {
    logger.info("{} in game-{} sent a message", getAttribute(header, USERNAME_HEADER), getAttribute(header, GAME_ID_HEADER));

    GameId gameId = getGameId(header);
    Username username = getUserName(header);
    gameService.toggleReady(gameId, username);
    messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(id), gameService.getPlayers(gameId));
    messagingTemplate.convertAndSend(TopicUtils.createChatTopic(id), createBotMessage(createReadinessMessage(gameId, username)));
    countdownService.handleBothPlayersReady(gameId, username);
  }

  private String createReadinessMessage(GameId gameId, Username username) {
    return gameService.getPlayer(gameId, username)
        .map(player -> player.isReady() ? format("%s is ready", username) : format("%s cancelled readiness", username))
        .orElseThrow();
  }

  private void handleUserDisconnect(GameId gameId, Player player) {
    switch (player.getAgency()) {
      case PLAYER_1 -> {
        messagingTemplate.convertAndSend(TopicUtils.createGameStateTopic(gameId), Notification.CREATOR_DISCONNECT);
        gameService.deleteGame(gameId);
      }
      case PLAYER_2 -> {
        Username username = player.getUsername();
        gameService.removeUser(gameId, username);
        messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameId), gameService.getPlayers(gameId));
        messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId), createBotMessage(format("%s left", username)));
      }
      default -> logger.error("player agency is not known {}", player.getAgency());
    }
  }
}

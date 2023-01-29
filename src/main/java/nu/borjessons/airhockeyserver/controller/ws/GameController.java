package nu.borjessons.airhockeyserver.controller.ws;

import java.time.ZonedDateTime;
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

// TODO can I remove ZonedDateTime from userMessage all together? Always set it on the frontend
@Controller
public class GameController {
  private static final String GAME_ID_HEADER = "gameId";
  private static final String USERNAME_HEADER = "username";
  private static final Logger logger = LoggerFactory.getLogger(GameController.class);

  private static String formatMessage(String message, Object... args) {
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

    // TODO user still falls out of store if page is reloaded, or does he? If I reload player1 frontend I cannot toggle readiness anymore
    if (gameService.addUserToGame(gameId, username)) {
      setAttribute(header, USERNAME_HEADER, username.toString());
      setAttribute(header, GAME_ID_HEADER, id);

      messagingTemplate.convertAndSend(TopicUtils.createChatTopic(id), TopicUtils.createConnectMessage(userMessage));
    }

    messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(id), gameService.getPlayers(gameId));
  }

  @MessageMapping("/game/{id}/disconnect")
  public void handleDisconnect(@DestinationVariable String id, @Payload UserMessage userMessage) {
    GameId gameId = new GameId(id);

    logger.info("disconnect event {}", userMessage);

    gameService.getPlayer(gameId, userMessage.username())
        .ifPresentOrElse(player -> handleUserDisconnect(gameId, userMessage, player),
            () -> logger.debug("rogue player disconnected from game {}", gameId));
  }

  // ZonedDateTime cannot be used here. Consider sending client Timezone on every request instead. Then we always construct datestamp on server
  @MessageMapping("/game/{id}/toggle-ready")
  public void toggleReady(@DestinationVariable String id, SimpMessageHeaderAccessor header) {
    logger.info("{} in game-{} sent a message", getAttribute(header, USERNAME_HEADER), getAttribute(header, GAME_ID_HEADER));

    GameId gameId = getGameId(header);
    Username username = getUserName(header);
    gameService.toggleReady(gameId, username);
    messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(id), gameService.getPlayers(gameId));
    messagingTemplate.convertAndSend(TopicUtils.createChatTopic(id),
        new UserMessage(TopicUtils.GAME_BOT, createReadinessMessage(gameId, username), ZonedDateTime.now()));
    countdownService.handleBothPlayersReady(gameId, username);
  }

  private String createReadinessMessage(GameId gameId, Username username) {
    return gameService.getPlayer(gameId, username)
        .map(player -> player.isReady() ? formatMessage("%s is ready", username) : formatMessage("%s cancelled readiness", username))
        .orElseThrow();
  }

  private void handleUserDisconnect(GameId gameId, UserMessage userMessage, Player player) {
    switch (player.getAgency()) {
      case PLAYER_1 -> {
        messagingTemplate.convertAndSend(TopicUtils.createGameStateTopic(gameId.toString()), Notification.CREATOR_DISCONNECT);
        gameService.deleteGame(gameId);
      }
      case PLAYER_2 -> {
        gameService.removeUser(gameId, userMessage.username());
        messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameId.toString()), gameService.getPlayers(gameId));
        messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId.toString()), TopicUtils.createDisconnectMessage(userMessage));
      }
      default -> logger.error("player agency is not known {}", player.getAgency());
    }
  }
}

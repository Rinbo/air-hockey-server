package nu.borjessons.airhockeyserver.controller.ws;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.GameState;
import nu.borjessons.airhockeyserver.model.Notification;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.UserMessage;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.service.api.GameService;

// TODO can I remove ZonedDateTime from userMessage all together? Always set it on the frontend
@Controller
public class GameController {
  private static final Username GAME_BOT = new Username("Game Bot");
  private static final String GAME_ID_HEADER = "gameId";
  private static final String USERNAME_HEADER = "username";
  private static final Logger logger = LoggerFactory.getLogger(GameController.class);

  private static String createChatTopic(String gameId) {
    return String.format("/topic/game/%s/chat", gameId);
  }

  private static UserMessage createConnectMessage(UserMessage userMessage) {
    return new UserMessage(GAME_BOT, userMessage.username() + " joined", userMessage.datetime());
  }

  private static UserMessage createDisconnectMessage(UserMessage userMessage) {
    return new UserMessage(GAME_BOT, userMessage.username() + " left", userMessage.datetime());
  }

  private static String createGameStateTopic(String gameId) {
    return String.format("/topic/game/%s/game-state", gameId);
  }

  private static String createPlayerTopic(String gameId) {
    return String.format("/topic/game/%s/players", gameId);
  }

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

  private final Map<GameId, Timer> countdownMap;
  private final GameService gameService;
  private final SimpMessagingTemplate messagingTemplate;

  public GameController(SimpMessagingTemplate messagingTemplate, GameService gameService) {
    this.countdownMap = new ConcurrentHashMap<>();
    this.messagingTemplate = messagingTemplate;
    this.gameService = gameService;
  }

  // TODO maybe all cases where I use the Username, I should grab it from the header instead
  // TODO add validation so that only the two players in the gameStore are allowed to send here
  @MessageMapping("/game/{id}/chat")
  public void handleChat(@DestinationVariable String id, @Payload UserMessage userMessage, SimpMessageHeaderAccessor header) {
    logger.info("{} in game-{} sent a message", getAttribute(header, USERNAME_HEADER), getAttribute(header, GAME_ID_HEADER));
    gameService.getGameStore(new GameId(id)).ifPresent(gameStore -> logger.info("gameStore state: {}", gameStore));

    messagingTemplate.convertAndSend(createChatTopic(id), userMessage);
  }

  @MessageMapping("/game/{id}/connect")
  public void handleConnect(@DestinationVariable String id, @Payload UserMessage userMessage, SimpMessageHeaderAccessor header) {
    Username username = userMessage.username();
    GameId gameId = new GameId(id);

    // TODO user still falls out of store if page is reloaded, or does he? If I reload player1 frontend I cannot toggle readiness anymore
    if (gameService.addUserToGame(gameId, username)) {
      setAttribute(header, USERNAME_HEADER, username.toString());
      setAttribute(header, GAME_ID_HEADER, id);

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

  // ZonedDateTime cannot be used here. Consider sending client Timezone on every request instead. Then we always construct datestamp on server
  @MessageMapping("/game/{id}/toggle-ready")
  public void toggleReady(@DestinationVariable String id, SimpMessageHeaderAccessor header) {
    logger.info("{} in game-{} sent a message", getAttribute(header, USERNAME_HEADER), getAttribute(header, GAME_ID_HEADER));

    GameId gameId = getGameId(header);
    Username username = getUserName(header);
    gameService.toggleReady(gameId, username);
    messagingTemplate.convertAndSend(createPlayerTopic(id), gameService.getPlayers(gameId));
    messagingTemplate.convertAndSend(createChatTopic(id), new UserMessage(GAME_BOT, createReadinessMessage(gameId, username), ZonedDateTime.now()));
    handleBothPlayersReady(gameId);
  }

  private TimerTask createCountdownTask(GameId gameId, Timer timer, IntConsumer intConsumer, Runnable runnable) {
    return new TimerTask() {
      int count = 3;

      @Override
      public void run() {
        logger.info("timer called");
        intConsumer.accept(count--);
        if (count < 0) {
          runnable.run();
          timer.cancel();
          countdownMap.remove(gameId);
        }
      }
    };
  }

  private String createReadinessMessage(GameId gameId, Username username) {
    return gameService.getPlayer(gameId, username)
        .map(player -> player.isReady() ? formatMessage("%s is ready", username) : formatMessage("%s cancelled readiness", username))
        .orElseThrow();
  }

  private void handleBothPlayersReady(GameId gameId) {
    if (gameService.getPlayers(gameId).stream().allMatch(Player::isReady) && !countdownMap.containsKey(gameId)) {
      Timer timer = new Timer(gameId.toString());
      TimerTask timerTask = createCountdownTask(gameId, timer, count -> messagingTemplate.convertAndSend(createChatTopic(gameId.toString()),
              new UserMessage(GAME_BOT, "Game starts in " + count, ZonedDateTime.now())),
          () -> {
            gameService.getGameStore(gameId).ifPresent(gameStore -> gameStore.setGameState(GameState.GAME_RUNNING));
            messagingTemplate.convertAndSend(createGameStateTopic(gameId.toString()), GameState.GAME_RUNNING);
          });

      timer.schedule(timerTask, 0, 1000);

      countdownMap.put(gameId, timer);
    } else if (countdownMap.containsKey(gameId)) {
      countdownMap.get(gameId).cancel();
      countdownMap.remove(gameId);
      messagingTemplate.convertAndSend(createChatTopic(gameId.toString()), new UserMessage(GAME_BOT, "Countdown cancelled", ZonedDateTime.now()));
    }
  }

  private void handleUserDisconnect(String id, UserMessage userMessage, GameId gameId, Player player) {
    switch (player.getAgency()) {
      case PLAYER_1 -> {
        messagingTemplate.convertAndSend(createGameStateTopic(id), Notification.CREATOR_DISCONNECT);
        gameService.deleteGame(gameId);
      }
      case PLAYER_2 -> {
        gameService.removeUser(gameId, userMessage.username());
        messagingTemplate.convertAndSend(createPlayerTopic(id), gameService.getPlayers(gameId));
        messagingTemplate.convertAndSend(createChatTopic(id), createDisconnectMessage(userMessage));
      }
      default -> logger.error("player agency is not known {}", player.getAgency());
    }
  }
}

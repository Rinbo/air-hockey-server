package nu.borjessons.airhockeyserver.controller;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import nu.borjessons.airhockeyserver.controller.security.GameValidator;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.model.AuthRecord;
import nu.borjessons.airhockeyserver.model.Game;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.GameState;
import nu.borjessons.airhockeyserver.model.Notification;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.UserMessage;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.GameStore;
import nu.borjessons.airhockeyserver.service.api.CountdownService;
import nu.borjessons.airhockeyserver.service.api.GameService;
import nu.borjessons.airhockeyserver.utils.HeaderUtils;
import nu.borjessons.airhockeyserver.utils.TopicUtils;

@Controller
public class GameController {
  private static final Logger logger = LoggerFactory.getLogger(GameController.class);

  private final CountdownService countdownService;
  private final GameService gameService;
  private final GameValidator gameValidator;
  private final SimpMessagingTemplate messagingTemplate;

  public GameController(CountdownService countdownService, GameService gameService, GameValidator gameValidator, SimpMessagingTemplate messagingTemplate) {
    this.countdownService = countdownService;
    this.gameService = gameService;
    this.gameValidator = gameValidator;
    this.messagingTemplate = messagingTemplate;
  }

  private static String format(String message, Object... args) {
    return String.format(Locale.ROOT, message, args);
  }

  @GetMapping("/games")
  @ResponseBody
  public Collection<Game> getGames() {
    return getGameList();
  }

  @MessageMapping("/game/{id}/chat")
  public void handleChat(@DestinationVariable String id, @Payload UserMessage userMessage, SimpMessageHeaderAccessor header) {
    gameValidator.validateUser(header, id);

    messagingTemplate.convertAndSend(TopicUtils.createChatTopic(id), userMessage);
  }

  @MessageMapping("/game/{id}/connect")
  public void handleConnect(@DestinationVariable String id, @Payload UserMessage userMessage, SimpMessageHeaderAccessor header) {
    Username username = userMessage.username();
    GameId gameId = new GameId(id);

    logger.info("{} connected", username);

    HeaderUtils.setUsername(header, username);
    HeaderUtils.setGameId(header, gameId);

    if (gameService.addUserToGame(gameId, username)) {
      logger.info("{} added to gameStore", username);
      messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId), TopicUtils.createBotMessage(format("%s joined", username)));
      messagingTemplate.convertAndSend(TopicUtils.GAMES_TOPIC, getGameList());
    }

    gameService
        .getPlayer(gameId, username)
        .ifPresentOrElse(
            player -> messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameId), gameService.getPlayers(gameId)),
            () -> messagingTemplate.convertAndSend(TopicUtils.createUserTopic(username), "FORBIDDEN")
        );
  }

  @MessageMapping("/game/{id}/disconnect")
  public void handleDisconnect(@DestinationVariable String id, SimpMessageHeaderAccessor header) {
    GameId gameId = HeaderUtils.getGameId(header);
    Username username = HeaderUtils.getUsername(header);

    logger.info("disconnect event {}", username);

    countdownService.cancelTimer(new GameId(id));
    gameService.getPlayer(gameId, username).ifPresent(player -> handleUserDisconnect(gameId, player));
    messagingTemplate.convertAndSend(TopicUtils.GAMES_TOPIC, getGameList());
  }

  @MessageMapping("/game/{id}/toggle-ready")
  public void toggleReady(@DestinationVariable String id, SimpMessageHeaderAccessor header) {
    AuthRecord authRecord = gameValidator.validateUser(header, id);
    GameId gameId = authRecord.gameId();
    Username username = authRecord.username();

    gameService.toggleReady(gameId, username);
    messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(id), gameService.getPlayers(gameId));
    messagingTemplate.convertAndSend(TopicUtils.createChatTopic(id), TopicUtils.createBotMessage(createReadinessMessage(gameId, username)));
    countdownService.handleBothPlayersReady(gameId, username);
  }

  @MessageMapping("/game/{id}/update-handle")
  public void updateHandle(@DestinationVariable String id, @Payload Position position, SimpMessageHeaderAccessor header) {
    AuthRecord authRecord = gameValidator.validateUser(header, id);
    GameId gameId = authRecord.gameId();
    Username username = authRecord.username();

    gameService.getGameStore(gameId).ifPresent(gameStore ->
        gameStore.getPlayer(username).ifPresent(player -> gameStore.updateHandle(position, player.getAgency()))
    );
  }

  private String createReadinessMessage(GameId gameId, Username username) {
    return gameService.getPlayer(gameId, username)
        .map(player -> player.isReady() ? format("%s is ready", username) : format("%s cancelled readiness", username))
        .orElseThrow();
  }

  private List<Game> getGameList() {
    return gameService.getGameStores().stream().map(Game::new).toList();
  }

  private void handleUserDisconnect(GameId gameId, Player player) {
    switch (player.getAgency()) {
      case PLAYER_1 -> {
        messagingTemplate.convertAndSend(TopicUtils.createGameStateTopic(gameId), Notification.PLAYER_1_DISCONNECT);
        gameService.deleteGame(gameId);
      }
      case PLAYER_2 -> {
        Username username = player.getUsername();

        messagingTemplate.convertAndSend(TopicUtils.createGameStateTopic(gameId), Notification.LOBBY);
        messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId), TopicUtils.createBotMessage(format("%s left", username)));

        gameService.removeUser(gameId, username);
        gameService.getGameStore(gameId).ifPresent(this::transitionIfRunning);
        messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameId), gameService.getPlayers(gameId));
      }
      default -> logger.error("player agency is not known {}", player.getAgency());
    }
  }

  private void transitionIfRunning(GameStore gameStore) {
    GameState gameState = gameStore.getGameState();
    if (gameState == GameState.GAME_RUNNING) {
      gameStore.transition(GameState.LOBBY);
      GameId gameId = gameStore.getGameId();
      gameStore.getPlayers().forEach(Player::toggleReady);
      gameStore.terminate();
      messagingTemplate.convertAndSend(TopicUtils.createGameStateTopic(gameId), Notification.PLAYER_2_DISCONNECT);
    }
  }
}

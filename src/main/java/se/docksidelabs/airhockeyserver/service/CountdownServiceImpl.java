package se.docksidelabs.airhockeyserver.service;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import se.docksidelabs.airhockeyserver.gateway.GatewayClient;
import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.model.Notification;
import se.docksidelabs.airhockeyserver.model.Player;
import se.docksidelabs.airhockeyserver.model.UserMessage;
import se.docksidelabs.airhockeyserver.model.Username;
import se.docksidelabs.airhockeyserver.service.api.CountdownService;
import se.docksidelabs.airhockeyserver.service.api.GameService;
import se.docksidelabs.airhockeyserver.utils.TopicUtils;
import se.docksidelabs.airhockeyserver.transport.BoardTransport;

@Service
public class CountdownServiceImpl implements CountdownService {
  private final BoardTransport boardTransport;
  private final Map<GameId, Timer> countdownMap;
  private final GatewayClient gatewayClient;
  private final GameService gameService;
  private final SimpMessagingTemplate messagingTemplate;

  public CountdownServiceImpl(GameService gameService, SimpMessagingTemplate messagingTemplate,
      BoardTransport boardTransport, GatewayClient gatewayClient) {
    Objects.requireNonNull(gameService, "gameService must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
    Objects.requireNonNull(boardTransport, "boardTransport must not be null");
    Objects.requireNonNull(gatewayClient, "gatewayClient must not be null");

    this.boardTransport = boardTransport;
    this.countdownMap = new ConcurrentHashMap<>();
    this.gatewayClient = gatewayClient;
    this.gameService = gameService;
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void cancelTimer(GameId gameId) {
    Timer timer = countdownMap.remove(gameId);
    if (timer != null) {
      sendCancelMessage(gameId);
      timer.cancel();
    }
  }

  @Override
  public void handleBothPlayersReady(GameId gameId, Username username) {
    Collection<Player> players = gameService.getPlayers(gameId);
    boolean allReady = players.size() == 2 && players.stream().allMatch(Player::isReady);

    if (allReady && !countdownMap.containsKey(gameId)) {
      Timer timer = new Timer(gameId.toString());
      TimerTask timerTask = createCountdownTask(gameId, timer);
      timer.schedule(timerTask, 0, 1000);

      countdownMap.put(gameId, timer);
    } else {
      Timer existing = countdownMap.remove(gameId);
      if (existing != null) {
        existing.cancel();
        sendCancelMessage(gameId);
      }
    }
  }

  private TimerTask createCountdownTask(GameId gameId, Timer timer) {
    return new TimerTask() {
      int count = 3;

      @Override
      public void run() {
        messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId),
            new UserMessage(TopicUtils.GAME_BOT, "Game starts in " + count--));
        if (count < 0) {
          gameService.getGameStore(gameId)
              .ifPresent(gameStore -> gameStore.startGame(messagingTemplate, boardTransport, gatewayClient));
          messagingTemplate.convertAndSend(TopicUtils.createGameStateTopic(gameId), Notification.GAME_RUNNING);
          timer.cancel();
          countdownMap.remove(gameId);
        }
      }
    };
  }

  private void sendCancelMessage(GameId gameId) {
    messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId.toString()),
        new UserMessage(TopicUtils.GAME_BOT, "Countdown cancelled"));
  }
}

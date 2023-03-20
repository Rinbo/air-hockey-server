package nu.borjessons.airhockeyserver.service;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Notification;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.UserMessage;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.service.api.CountdownService;
import nu.borjessons.airhockeyserver.service.api.GameService;
import nu.borjessons.airhockeyserver.utils.TopicUtils;

@Service
public class CountdownServiceImpl implements CountdownService {
  private final Map<GameId, Timer> countdownMap;
  private final GameService gameService;
  private final SimpMessagingTemplate messagingTemplate;

  public CountdownServiceImpl(GameService gameService, SimpMessagingTemplate messagingTemplate) {
    Objects.requireNonNull(gameService, "gameService must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");

    this.countdownMap = new ConcurrentHashMap<>();
    this.gameService = gameService;
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void cancelTimer(GameId gameId) {
    if (countdownMap.containsKey(gameId)) {
      sendCancelMessage(gameId);
      Timer timer = countdownMap.get(gameId);
      timer.cancel();
      countdownMap.remove(gameId);
    }
  }

  @Override
  public void handleBothPlayersReady(GameId gameId, Username username) {
    Collection<Player> players = gameService.getPlayers(gameId);

    if (players.size() == 2 && players.stream().allMatch(Player::isReady) && !countdownMap.containsKey(gameId)) {
      Timer timer = new Timer(gameId.toString());
      TimerTask timerTask = createCountdownTask(gameId, timer);
      timer.schedule(timerTask, 0, 1000);

      countdownMap.put(gameId, timer);
    } else if (countdownMap.containsKey(gameId)) {
      countdownMap.get(gameId).cancel();
      countdownMap.remove(gameId);
      sendCancelMessage(gameId);
    }
  }

  private TimerTask createCountdownTask(GameId gameId, Timer timer) {
    return new TimerTask() {
      int count = 3;

      @Override
      public void run() {
        messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId), new UserMessage(TopicUtils.GAME_BOT, "Game starts in " + count--));
        if (count < 0) {
          gameService.getGameStore(gameId).ifPresent(gameStore -> gameStore.startGame(messagingTemplate));
          messagingTemplate.convertAndSend(TopicUtils.createGameStateTopic(gameId), Notification.GAME_RUNNING);
          timer.cancel();
          countdownMap.remove(gameId);
        }
      }
    };
  }

  private void sendCancelMessage(GameId gameId) {
    messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId.toString()), new UserMessage(TopicUtils.GAME_BOT, "Countdown cancelled"));
  }
}

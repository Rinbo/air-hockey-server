package nu.borjessons.airhockeyserver.repository;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.game.BroadcastState;
import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.GameState;
import nu.borjessons.airhockeyserver.model.Notification;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.utils.TopicUtils;
import nu.borjessons.airhockeyserver.websocket.GameWebSocketHandler;

public class GameStoreConnector {
  private final GameStore gameStore;
  private final GameWebSocketHandler gameWebSocketHandler;
  private final SimpMessagingTemplate messagingTemplate;

  public GameStoreConnector(GameStore gameStore, SimpMessagingTemplate messagingTemplate,
      GameWebSocketHandler gameWebSocketHandler) {
    Objects.requireNonNull(gameStore, "gameStore must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
    Objects.requireNonNull(gameWebSocketHandler, "gameWebSocketHandler must not be null");

    this.gameStore = gameStore;
    this.messagingTemplate = messagingTemplate;
    this.gameWebSocketHandler = gameWebSocketHandler;
  }

  private static String printResult(Collection<Player> players) {
    StringBuilder stringBuilder = new StringBuilder("Result: ");

    for (Player player : players) {
      stringBuilder
          .append(player.getUsername().getTrimmed())
          .append(": ")
          .append(player.getScore())
          .append(", ");
    }

    return stringBuilder.substring(0, stringBuilder.length() - 2);
  }

  private static void updateGamesWon(Collection<Player> players) {
    Optional<Player> player1Optional = players.stream().filter(player -> player.getAgency() == Agency.PLAYER_1)
        .findFirst();
    Optional<Player> player2Optional = players.stream().filter(player -> player.getAgency() == Agency.PLAYER_2)
        .findFirst();

    player1Optional.ifPresent(player1 -> player2Optional.ifPresent(player2 -> {
      if (player1.getScore() > player2.getScore())
        player1.incrementGamesWon();
      if (player2.getScore() > player1.getScore())
        player2.incrementGamesWon();
    }));
  }

  /**
   * Broadcasts board state via raw binary WebSocket for minimal overhead.
   */
  public void broadcast(BroadcastState playerOneState, BroadcastState playerTwoState) {
    GameId gameId = gameStore.getGameId();
    gameWebSocketHandler.sendBoardState(gameId, Agency.PLAYER_1, playerOneState);
    gameWebSocketHandler.sendBoardState(gameId, Agency.PLAYER_2, playerTwoState);
  }

  public void gameComplete() {
    GameId gameId = gameStore.getGameId();
    Collection<Player> players = gameStore.getPlayers();

    messagingTemplate.convertAndSend(TopicUtils.createGameStateTopic(gameId), Notification.SCORE_SCREEN);
    messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId),
        TopicUtils.createBotMessage(printResult(players)));

    updateGamesWon(players);

    players.forEach(Player::toggleReady);
    messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameId), players);

    players.forEach(Player::resetScore);

    gameStore.transition(GameState.LOBBY);
    gameStore.terminate();
  }

  public void updatePlayerScore(Agency agency) {
    Collection<Player> players = gameStore.getPlayers();
    players.stream().filter(player -> player.getAgency() == agency).forEach(Player::score);
    messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameStore.getGameId()), players);
  }
}

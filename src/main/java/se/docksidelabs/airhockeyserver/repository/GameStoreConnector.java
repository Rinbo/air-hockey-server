package se.docksidelabs.airhockeyserver.repository;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import se.docksidelabs.airhockeyserver.game.BroadcastState;
import se.docksidelabs.airhockeyserver.gateway.GatewayClient;
import se.docksidelabs.airhockeyserver.model.Agency;
import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.model.GameState;
import se.docksidelabs.airhockeyserver.model.Notification;
import se.docksidelabs.airhockeyserver.model.Player;
import se.docksidelabs.airhockeyserver.utils.TopicUtils;
import se.docksidelabs.airhockeyserver.websocket.GameWebSocketHandler;

public class GameStoreConnector {
  private final GatewayClient gatewayClient;
  private final GameStore gameStore;
  private final GameWebSocketHandler gameWebSocketHandler;
  private final SimpMessagingTemplate messagingTemplate;

  public GameStoreConnector(GameStore gameStore, SimpMessagingTemplate messagingTemplate,
      GameWebSocketHandler gameWebSocketHandler, GatewayClient gatewayClient) {
    Objects.requireNonNull(gameStore, "gameStore must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
    Objects.requireNonNull(gameWebSocketHandler, "gameWebSocketHandler must not be null");
    Objects.requireNonNull(gatewayClient, "gatewayClient must not be null");

    this.gatewayClient = gatewayClient;
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

    // Report game played to gateway for each human player
    players.forEach(player -> gatewayClient.reportGamePlayed(player.getGatewayUserId()));

    players.forEach(Player::toggleReady);

    // Re-ready the AI so the next game starts when the human readies up
    if (gameStore.isAiMode()) {
      players.stream()
          .filter(p -> p.getAgency() == Agency.PLAYER_2)
          .forEach(Player::toggleReady);
    }

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

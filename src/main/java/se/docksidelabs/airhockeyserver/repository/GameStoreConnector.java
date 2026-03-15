package se.docksidelabs.airhockeyserver.repository;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import se.docksidelabs.airhockeyserver.game.BroadcastState;
import se.docksidelabs.airhockeyserver.game.properties.GameConstants;
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

    // Only report to gateway for human-vs-human games
    if (!gameStore.isAiMode()) {
      players.forEach(player -> gatewayClient.reportGamePlayed(player.getGatewayUserId()));

      // Report match result for ELO calculation and match history
      Player p1 = players.stream().filter(p -> p.getAgency() == Agency.PLAYER_1).findFirst().orElse(null);
      Player p2 = players.stream().filter(p -> p.getAgency() == Agency.PLAYER_2).findFirst().orElse(null);
      if (p1 != null && p2 != null) {
        int durationSeconds = (int) GameConstants.GAME_DURATION.toSeconds();
        gatewayClient.reportMatchResult(
            p1.getGatewayUserId(), p2.getGatewayUserId(),
            p1.getScore(), p2.getScore(), durationSeconds);
      }
    }

    // Explicitly reset all players to not-ready (don't toggle — that's fragile)
    players.forEach(p -> p.setReady(false));

    // Re-ready the AI so the next game starts when the human readies up
    if (gameStore.isAiMode()) {
      players.stream()
          .filter(p -> p.getAgency() == Agency.PLAYER_2)
          .forEach(p -> p.setReady(true));
    }

    messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameId), players);

    players.forEach(Player::resetScore);
    messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameId), players);

    gameStore.transition(GameState.LOBBY);
    gameStore.terminate();
  }

  public void updatePlayerScore(Agency agency) {
    Collection<Player> players = gameStore.getPlayers();
    players.stream().filter(player -> player.getAgency() == agency).forEach(Player::score);
    messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameStore.getGameId()), players);
  }
}

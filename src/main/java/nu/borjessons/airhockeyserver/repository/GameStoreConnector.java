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

public class GameStoreConnector {
  private final GameStore gameStore;
  private final SimpMessagingTemplate messagingTemplate;
  private final String playerOneTopic;
  private final String playerTwoTopic;

  public GameStoreConnector(GameStore gameStore, SimpMessagingTemplate messagingTemplate) {
    Objects.requireNonNull(gameStore, "gameStore must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");

    GameId gameId = gameStore.getGameId();
    this.gameStore = gameStore;
    this.messagingTemplate = messagingTemplate;
    this.playerOneTopic = String.format("/topic/game/%s/board-state/player-1", gameId);
    this.playerTwoTopic = String.format("/topic/game/%s/board-state/player-2", gameId);
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
    Optional<Player> player1Optional = players.stream().filter(player -> player.getAgency() == Agency.PLAYER_1).findFirst();
    Optional<Player> player2Optional = players.stream().filter(player -> player.getAgency() == Agency.PLAYER_2).findFirst();

    player1Optional.ifPresent(player1 -> player2Optional.ifPresent(player2 -> {
      if (player1.getScore() > player2.getScore()) player1.incrementGamesWon();
      if (player2.getScore() > player1.getScore()) player2.incrementGamesWon();
    }));
  }

  public void broadcast(BroadcastState playerOneState, BroadcastState playerTwoState) {
    messagingTemplate.convertAndSend(playerOneTopic, playerOneState);
    messagingTemplate.convertAndSend(playerTwoTopic, playerTwoState);
  }

  public void gameComplete() {
    GameId gameId = gameStore.getGameId();
    Collection<Player> players = gameStore.getPlayers();

    messagingTemplate.convertAndSend(TopicUtils.createGameStateTopic(gameId), Notification.SCORE_SCREEN);
    messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId), TopicUtils.createBotMessage(printResult(players)));

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

package nu.borjessons.airhockeyserver.repository;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.game.BroadcastState;
import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.utils.TopicUtils;

public class GameStoreController {
  private final GameId gameId;
  private final SimpMessagingTemplate messagingTemplate;
  private final String playerOneTopic;
  private final Supplier<Collection<Player>> playerSupplier;
  private final String playerTwoTopic;
  private final Runnable terminationCallback;

  public GameStoreController(GameId gameId, SimpMessagingTemplate messagingTemplate, Supplier<Collection<Player>> playerSupplier,
      Runnable terminationCallback) {
    Objects.requireNonNull(gameId, "gameId must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
    Objects.requireNonNull(playerSupplier, "playerSupplier must not be null");
    Objects.requireNonNull(terminationCallback, "terminationCallback must not be null");

    this.gameId = gameId;
    this.messagingTemplate = messagingTemplate;
    this.playerOneTopic = String.format("/topic/game/%s/board-state/player-1", gameId);
    this.playerSupplier = playerSupplier;
    this.playerTwoTopic = String.format("/topic/game/%s/board-state/player-2", gameId);
    this.terminationCallback = terminationCallback;
  }

  public void broadcast(BroadcastState playerOneState, BroadcastState playerTwoState) {
    messagingTemplate.convertAndSend(playerOneTopic, playerOneState);
    messagingTemplate.convertAndSend(playerTwoTopic, playerTwoState);
  }

  public void terminateGame() {
    terminationCallback.run();
  }

  public void updatePlayerScore(Agency agency) {
    Collection<Player> players = playerSupplier.get();
    players.stream().filter(player -> player.getAgency() == agency).forEach(Player::score);
    messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameId), players);
  }
}

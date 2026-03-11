package se.docksidelabs.airhockeyserver.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import se.docksidelabs.airhockeyserver.model.Game;
import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.model.GameState;
import se.docksidelabs.airhockeyserver.model.Notification;
import se.docksidelabs.airhockeyserver.model.Player;
import se.docksidelabs.airhockeyserver.model.Username;
import se.docksidelabs.airhockeyserver.repository.GameStore;
import se.docksidelabs.airhockeyserver.service.api.GameService;
import se.docksidelabs.airhockeyserver.utils.AppUtils;
import se.docksidelabs.airhockeyserver.utils.TopicUtils;

@Service
public class GameServiceImpl implements GameService {
  private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

  private final Map<GameId, GameStore> gameStoreMap;
  private final SimpMessagingTemplate messagingTemplate;

  public GameServiceImpl(Map<GameId, GameStore> gameStoreMap, SimpMessagingTemplate messagingTemplate) {
    this.gameStoreMap = gameStoreMap;
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public boolean addUserToGame(GameId gameId, Username username, String gatewayUserId) {
    GameStore gameStore = gameStoreMap.computeIfAbsent(gameId, GameStore::new);
    return gameStore.addPlayer(username, gatewayUserId);
  }

  @Override
  public void deleteGame(GameId gameId) {
    GameStore gameStore = gameStoreMap.remove(gameId);
    gameStore.terminate();
    logger.info("removed gameStore {}", gameStore);
  }

  @Override
  public GameState getGameState(GameId gameId) {
    GameStore gameStore = gameStoreMap.get(gameId);
    return gameStore != null ? gameStore.getGameState() : GameState.LOBBY;
  }

  @Override
  public Optional<GameStore> getGameStore(GameId gameId) {
    return Optional.ofNullable(gameStoreMap.get(gameId));
  }

  @Override
  public Collection<GameStore> getGameStores() {
    return gameStoreMap.values();
  }

  @Override
  public Optional<Player> getPlayer(GameId gameId, Username username) {
    GameStore gameStore = gameStoreMap.get(gameId);
    return gameStore != null ? gameStore.getPlayer(username) : Optional.empty();
  }

  @Override
  public Collection<Player> getPlayers(GameId gameId) {
    return getGameStore(gameId).map(GameStore::getPlayers).orElseThrow();
  }

  @Override
  public void handleUserDisconnect(GameId gameId, Username username) {
    getPlayer(gameId, username).ifPresent(player -> handleUserDisconnect(gameId, player));
    messagingTemplate.convertAndSend(TopicUtils.GAMES_TOPIC, getGameStores().stream().map(Game::new).toList());
  }

  @Override
  public void removeUser(GameId gameId, Username username) {
    getGameStore(gameId)
        .ifPresentOrElse(gameStore -> gameStore.getPlayer(username).ifPresent(gameStore::removePlayer),
            () -> logger.warn("tried to remove a player from store with non-existent gameId: {}", gameId));

  }

  @Override
  public void setReady(GameId gameId, Username userName, boolean ready) {
    getGameStore(gameId)
        .ifPresent(gameStore -> gameStore.getPlayer(userName)
            .ifPresent(player -> gameStore.setPlayerReady(player, ready)));
  }

  @Override
  public void toggleReady(GameId gameId, Username userName) {
    getGameStore(gameId)
        .ifPresent(gameStore -> gameStore.getPlayer(userName)
            .ifPresent(gameStore::togglePlayerReadiness));
  }

  private void handleUserDisconnect(GameId gameId, Player player) {
    switch (player.getAgency()) {
      case PLAYER_1 -> {
        messagingTemplate.convertAndSend(TopicUtils.createGameStateTopic(gameId), Notification.PLAYER_1_DISCONNECT);
        deleteGame(gameId);
      }
      case PLAYER_2 -> {
        Username username = player.getUsername();

        messagingTemplate.convertAndSend(TopicUtils.createGameStateTopic(gameId), Notification.LOBBY);
        messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId),
            TopicUtils.createBotMessage(AppUtils.format("%s left", username.getTrimmed())));

        removeUser(gameId, username);
        getGameStore(gameId).ifPresent(this::transitionIfRunning);
        messagingTemplate.convertAndSend(TopicUtils.createPlayerTopic(gameId), getPlayers(gameId));
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

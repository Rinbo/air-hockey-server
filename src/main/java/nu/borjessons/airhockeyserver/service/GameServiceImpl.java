package nu.borjessons.airhockeyserver.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import nu.borjessons.airhockeyserver.model.Game;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.GameState;
import nu.borjessons.airhockeyserver.model.Notification;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.GameStore;
import nu.borjessons.airhockeyserver.service.api.GameService;
import nu.borjessons.airhockeyserver.utils.AppUtils;
import nu.borjessons.airhockeyserver.utils.TopicUtils;

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
  public boolean addUserToGame(GameId gameId, Username username) {
    GameStore gameStore = gameStoreMap.computeIfAbsent(gameId, GameStore::new);
    return gameStore.addPlayer(username);
  }

  @Override
  public void deleteGame(GameId gameId) {
    GameStore gameStore = gameStoreMap.remove(gameId);
    gameStore.terminate();
    logger.info("removed gameStore {}", gameStore);
  }

  @Override
  public GameState getGameState(GameId gameId) {
    if (gameStoreMap.containsKey(gameId)) {
      return gameStoreMap.get(gameId).getGameState();
    }

    return GameState.LOBBY;
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
    if (gameStoreMap.containsKey(gameId)) {
      return gameStoreMap.get(gameId).getPlayer(username);
    }

    return Optional.empty();
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
        messagingTemplate.convertAndSend(TopicUtils.createChatTopic(gameId), TopicUtils.createBotMessage(AppUtils.format("%s left", username.getTrimmed())));

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

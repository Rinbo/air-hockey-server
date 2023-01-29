package nu.borjessons.airhockeyserver.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.GameStore;
import nu.borjessons.airhockeyserver.service.api.GameService;

@Service
public class GameServiceImpl implements GameService {
  private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

  private final Map<GameId, GameStore> gameStoreMap;

  public GameServiceImpl(Map<GameId, GameStore> gameStoreMap) {
    this.gameStoreMap = gameStoreMap;
  }

  @Override
  public boolean addUserToGame(GameId gameId, Username username) {
    GameStore gameStore = gameStoreMap.computeIfAbsent(gameId, GameStore::new);
    return gameStore.addPlayer(username);
  }

  @Override
  public void deleteGame(GameId gameId) {
    GameStore gameStore = gameStoreMap.remove(gameId);
    logger.info("removed gameStore {}", gameStore);
  }

  @Override
  public Optional<GameStore> getGameStore(GameId gameId) {
    return Optional.ofNullable(gameStoreMap.get(gameId));
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
}

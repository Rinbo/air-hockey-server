package nu.borjessons.airhockeyserver.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import nu.borjessons.airhockeyserver.model.Agency;
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
    Set<Player> players = gameStoreMap.computeIfAbsent(gameId, GameStore::new).getPlayers();

    return switch (players.size()) {
      case 0 -> players.add(new Player(Agency.PLAYER_1, username));
      case 1 -> players.add(new Player(Agency.PLAYER_2, username));
      default -> false;
    };
  }

  @Override
  public void deleteGame(GameId gameId) {
    GameStore gameStore = gameStoreMap.remove(gameId);
    logger.info("removed gameStore {}", gameStore);
  }

  @Override
  public Optional<Player> getPlayer(GameId gameId, Username username) {
    if (gameStoreMap.containsKey(gameId)) {
      return gameStoreMap.get(gameId).getPlayer(username);
    }

    return Optional.empty();
  }

  @Override
  public Set<Player> getPlayers(GameId gameId) {
    if (gameStoreMap.containsKey(gameId)) {
      return gameStoreMap.get(gameId).getPlayers();
    }

    logger.warn("tried to get players from non-existant game {}", gameId);
    return Set.of();
  }

  @Override
  public void removeUser(GameId gameId, Username username) {
    GameStore gameStore = gameStoreMap.get(gameId);

    if (gameStore != null) {
      gameStore.getPlayer(username).ifPresent(gameStore::removePlayer);
      return;
    }

    logger.warn("tried to remove a player from store with non-existent gameId: {}", gameId);
  }
}

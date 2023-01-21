package nu.borjessons.airhockeyserver.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import nu.borjessons.airhockeyserver.model.Agent;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.service.api.GameService;

@Service
public class GameServiceImpl implements GameService {
  private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

  private final Map<GameId, Set<Player>> gameStore;

  public GameServiceImpl(Map<GameId, Set<Player>> gameStore) {
    this.gameStore = gameStore;
  }

  @Override
  public boolean addUserToGame(GameId gameId, Username username) {
    Set<Player> set = gameStore.computeIfAbsent(gameId, k -> new HashSet<>());
    return switch (set.size()) {
      case 0 -> set.add(new Player(username, Agent.PLAYER_1));
      case 1 -> set.add(new Player(username, Agent.PLAYER_2));
      default -> false;
    };
  }

  @Override
  public void deleteGame(GameId gameId) {
    Set<Player> players = gameStore.remove(gameId);
    logger.info("removed game {}, with players {}", gameId, players);
  }

  @Override
  public Optional<Player> getPlayer(GameId gameId, Username username) {
    if (gameStore.containsKey(gameId)) {
      return gameStore.get(gameId).stream().filter(player -> player.username().equals(username)).findFirst();
    }
    
    return Optional.empty();
  }

  @Override
  public Set<Player> getPlayers(GameId gameId) {
    return gameStore.get(gameId);
  }

  @Override
  public void removeUser(GameId gameId, Username username) {
    /*gameStore.computeIfPresent(gameId, (gid, players) -> {
      players.removeIf(player -> player.username().equals(username));
      return players;
    });*/

    Set<Player> players = gameStore.get(gameId);

    if (players != null) {
      boolean wasRemoved = players.removeIf(player -> player.username().equals(username));
      if (!wasRemoved) logger.warn("player {} was not in game: {}", username, players);
      return;
    }

    logger.warn("tried to remove a player from store with non-existent gameId: {}", gameId);
  }
}

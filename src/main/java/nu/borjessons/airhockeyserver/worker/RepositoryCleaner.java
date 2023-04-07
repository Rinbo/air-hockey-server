package nu.borjessons.airhockeyserver.worker;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.GameStore;
import nu.borjessons.airhockeyserver.repository.UserStore;
import nu.borjessons.airhockeyserver.service.api.GameService;

public class RepositoryCleaner {
  private static final Logger logger = LoggerFactory.getLogger(RepositoryCleaner.class);

  private final GameService gameService;
  private final Duration threshold;
  private final UserStore userStore;

  public RepositoryCleaner(GameService gameService, Duration threshold, UserStore userStore) {
    this.gameService = gameService;
    this.threshold = threshold;
    this.userStore = userStore;
  }

  @Scheduled(fixedRate = 5000)
  public void cleanUp() {
    List<Username> idleUsers = userStore.getAll().stream().filter(this::shouldBeDropped).toList();

    Collection<GameStore> gameStores = gameService.getGameStores();
    gameStores.forEach(gameStore -> removeIdlePlayers(gameStore, idleUsers));

    List<GameStore> emptyGameStores = gameStores.stream().filter(gameStore -> gameStore.getPlayers().isEmpty()).toList();
    emptyGameStores.forEach(gameStore -> gameService.deleteGame(gameStore.getGameId()));

    idleUsers.forEach(username -> removeUser(userStore, username));
  }

  private void removeIdlePlayers(GameStore gameStore, List<Username> usernames) {
    Collection<Player> players = gameStore.getPlayers();
    List<Player> idlePlayers = players.stream().filter(player -> usernames.contains(player.getUsername())).toList();
    idlePlayers.forEach(player -> gameService.handleUserDisconnect(gameStore.getGameId(), player.getUsername()));
  }

  private void removeUser(UserStore userStore, Username username) {
    userStore.removeUser(username);
    logger.info("removing user {} due to inactivity", username);
  }

  private boolean shouldBeDropped(Username username) {
    return Instant.now().isAfter(username.getInstant().plus(threshold));
  }
}

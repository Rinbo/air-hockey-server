package se.docksidelabs.airhockeyserver.worker;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import se.docksidelabs.airhockeyserver.repository.GameStore;
import se.docksidelabs.airhockeyserver.service.api.GameService;

/**
 * Periodically cleans up empty game stores.
 * <p>
 * User presence/idle detection is now handled by the gateway's Valkey-backed
 * presence registry (TTL-based expiry). This cleaner only removes game stores
 * that have no players.
 */
public class RepositoryCleaner {
  private static final Logger logger = LoggerFactory.getLogger(RepositoryCleaner.class);

  private final GameService gameService;

  public RepositoryCleaner(GameService gameService) {
    this.gameService = gameService;
  }

  @Scheduled(fixedRate = 5000)
  public void cleanUp() {
    Collection<GameStore> gameStores = gameService.getGameStores();

    List<GameStore> emptyGameStores = gameStores.stream()
        .filter(gameStore -> gameStore.getPlayers().isEmpty())
        .toList();

    emptyGameStores.forEach(gameStore -> {
      gameService.deleteGame(gameStore.getGameId());
      logger.info("Removed empty game: {}", gameStore.getGameId());
    });
  }
}

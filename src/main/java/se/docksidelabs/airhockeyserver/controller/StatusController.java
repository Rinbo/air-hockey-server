package se.docksidelabs.airhockeyserver.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.repository.GameStore;

/**
 * Exposes the server's current capacity so the gateway can make
 * informed scheduling decisions.
 */
@RestController
public class StatusController {

  private final Map<GameId, GameStore> gameStoreMap;
  private final String machineId;
  private final int maxConcurrentGames;
  private final String region;

  public StatusController(
      Map<GameId, GameStore> gameStoreMap,
      @Value("${server.machine-id:${FLY_MACHINE_ID:local}}") String machineId,
      @Value("${server.max-concurrent-games:20}") int maxConcurrentGames,
      @Value("${server.region:${FLY_REGION:local}}") String region) {
    this.gameStoreMap = gameStoreMap;
    this.machineId = machineId;
    this.maxConcurrentGames = maxConcurrentGames;
    this.region = region;
  }

  @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
  public ServerStatus getStatus() {
    return new ServerStatus(machineId, gameStoreMap.size(), maxConcurrentGames, region);
  }

  public record ServerStatus(String machineId, int activeGames, int maxGames, String region) {
  }
}

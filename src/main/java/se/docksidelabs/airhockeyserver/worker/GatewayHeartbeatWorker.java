package se.docksidelabs.airhockeyserver.worker;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import se.docksidelabs.airhockeyserver.gateway.GatewayClient;
import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.repository.GameStore;

/**
 * Periodically reports this server's capacity to the gateway so it can
 * make informed scheduling and scale-down decisions.
 */
public class GatewayHeartbeatWorker {
  private static final Logger logger = LoggerFactory.getLogger(GatewayHeartbeatWorker.class);

  private final GatewayClient gatewayClient;
  private final Map<GameId, GameStore> gameStoreMap;
  private final String machineId;
  private final int maxConcurrentGames;
  private final String region;

  public GatewayHeartbeatWorker(
      GatewayClient gatewayClient,
      Map<GameId, GameStore> gameStoreMap,
      String machineId,
      int maxConcurrentGames,
      String region) {
    this.gatewayClient = gatewayClient;
    this.gameStoreMap = gameStoreMap;
    this.machineId = machineId;
    this.maxConcurrentGames = maxConcurrentGames;
    this.region = region;
  }

  @Scheduled(fixedRate = 10_000)
  public void sendHeartbeat() {
    int activeGames = gameStoreMap.size();
    try {
      gatewayClient.sendHeartbeat(machineId, activeGames, maxConcurrentGames, region);
    } catch (Exception e) {
      logger.warn("Failed to send heartbeat to gateway", e);
    }
  }
}

package se.docksidelabs.airhockeyserver.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import se.docksidelabs.airhockeyserver.gateway.GatewayClient;
import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.repository.GameStore;
import se.docksidelabs.airhockeyserver.repository.UserStore;
import se.docksidelabs.airhockeyserver.service.api.GameService;
import se.docksidelabs.airhockeyserver.worker.GatewayHeartbeatWorker;
import se.docksidelabs.airhockeyserver.worker.PingWorker;
import se.docksidelabs.airhockeyserver.worker.RepositoryCleaner;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class AppConfig {
  @Bean
  ObjectMapper objectMapper() {
    return JsonMapper.builder()
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();
  }

  @Bean
  Map<GameId, GameStore> createGameStoreMap() {
    return new ConcurrentHashMap<>();
  }

  @Bean
  PingWorker createPingWorker(SimpMessagingTemplate messagingTemplate, UserStore userStore) {
    return new PingWorker(messagingTemplate, userStore);
  }

  @Bean
  RepositoryCleaner createRepositoryCleaner(GameService gameService, UserStore userStore) {
    return new RepositoryCleaner(gameService, Duration.ofMinutes(1), userStore);
  }

  @Bean
  GatewayHeartbeatWorker createGatewayHeartbeatWorker(
      GatewayClient gatewayClient,
      Map<GameId, GameStore> gameStoreMap,
      @Value("${server.machine-id:${FLY_MACHINE_ID:local}}") String machineId,
      @Value("${server.max-concurrent-games:20}") int maxConcurrentGames,
      @Value("${server.region:${FLY_REGION:local}}") String region) {
    return new GatewayHeartbeatWorker(gatewayClient, gameStoreMap, machineId, maxConcurrentGames, region);
  }

  @Bean
  UserStore createUserstore() {
    return new UserStore();
  }
}

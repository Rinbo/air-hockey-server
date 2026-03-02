package se.docksidelabs.airhockeyserver.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.repository.GameStore;
import se.docksidelabs.airhockeyserver.repository.UserStore;
import se.docksidelabs.airhockeyserver.service.api.GameService;
import se.docksidelabs.airhockeyserver.worker.PingWorker;
import se.docksidelabs.airhockeyserver.worker.RepositoryCleaner;

@Configuration
public class AppConfig {
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
  UserStore createUserstore() {
    return new UserStore();
  }
}

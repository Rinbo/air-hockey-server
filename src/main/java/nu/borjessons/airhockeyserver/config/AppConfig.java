package nu.borjessons.airhockeyserver.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.repository.GameStore;
import nu.borjessons.airhockeyserver.repository.UserStore;
import nu.borjessons.airhockeyserver.service.api.GameService;
import nu.borjessons.airhockeyserver.worker.PingWorker;
import nu.borjessons.airhockeyserver.worker.RepositoryCleaner;

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

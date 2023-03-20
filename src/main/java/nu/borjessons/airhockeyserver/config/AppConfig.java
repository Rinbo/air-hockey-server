package nu.borjessons.airhockeyserver.config;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import nu.borjessons.airhockeyserver.controller.dezerializer.GameIdDeserializer;
import nu.borjessons.airhockeyserver.controller.dezerializer.UsernameDeserializer;
import nu.borjessons.airhockeyserver.controller.serializer.GameIdSerializer;
import nu.borjessons.airhockeyserver.controller.serializer.UsernameSerializer;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.GameStore;
import nu.borjessons.airhockeyserver.repository.UserStore;

@Configuration
public class AppConfig {
  @Bean
  Map<GameId, GameStore> createGameStore() {
    return new ConcurrentHashMap<>();
  }

  @Bean
  ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    SimpleModule simpleModule = new SimpleModule();

    simpleModule.addSerializer(GameId.class, new GameIdSerializer());
    simpleModule.addSerializer(Username.class, new UsernameSerializer());

    simpleModule.addDeserializer(GameId.class, new GameIdDeserializer());
    simpleModule.addDeserializer(Username.class, new UsernameDeserializer());

    objectMapper.registerModules(new JavaTimeModule(), simpleModule);
    return objectMapper;
  }

  @Bean
  UserStore createUserstore() {
    return new UserStore(new HashSet<>());
  }
}

package nu.borjessons.airhockeyserver.config;

import java.util.Map;
import java.util.Set;
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
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;

@Configuration
public class AppConfig {
  @Bean
  Map<GameId, Set<Player>> createGameStore() {
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
}

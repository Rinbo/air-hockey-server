package nu.borjessons.airhockeyserver.controller.dezerializer;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import nu.borjessons.airhockeyserver.model.GameId;

public class GameIdDeserializer extends ValueDeserializer<GameId> {
  @Override
  public GameId deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
    return new GameId(jsonParser.getString());
  }
}

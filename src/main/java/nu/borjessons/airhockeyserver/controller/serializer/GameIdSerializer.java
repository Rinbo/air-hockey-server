package nu.borjessons.airhockeyserver.controller.serializer;

import java.io.IOException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import nu.borjessons.airhockeyserver.model.GameId;

public class GameIdSerializer extends ValueSerializer<GameId> {
  @Override
  public void serialize(GameId gameId, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
    jsonGenerator.writeString(gameId.toString());
  }
}

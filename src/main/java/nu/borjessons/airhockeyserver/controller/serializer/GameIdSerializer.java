package nu.borjessons.airhockeyserver.controller.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import nu.borjessons.airhockeyserver.model.GameId;

public class GameIdSerializer extends JsonSerializer<GameId> {
  @Override
  public void serialize(GameId gameId, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeString(gameId.toString());
  }
}

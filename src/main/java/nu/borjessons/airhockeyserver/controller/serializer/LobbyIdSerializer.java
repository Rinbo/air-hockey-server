package nu.borjessons.airhockeyserver.controller.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import nu.borjessons.airhockeyserver.model.LobbyId;

public class LobbyIdSerializer extends JsonSerializer<LobbyId> {
  @Override
  public void serialize(LobbyId lobbyId, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeString(lobbyId.toString());
  }
}

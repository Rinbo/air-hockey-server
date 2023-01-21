package nu.borjessons.airhockeyserver.controller.dezerializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import nu.borjessons.airhockeyserver.model.LobbyId;

public class LobbyIdDeserializer extends JsonDeserializer<LobbyId> {
  @Override
  public LobbyId deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
    return LobbyId.ofString(jsonNode.asText());
  }
}

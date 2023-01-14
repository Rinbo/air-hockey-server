package nu.borjessons.airhockeyserver.controller.dezerializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import nu.borjessons.airhockeyserver.model.Username;

public class UsernameDeserializer extends JsonDeserializer<Username> {
  @Override
  public Username deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
    return new Username(jsonNode.asText());
  }
}

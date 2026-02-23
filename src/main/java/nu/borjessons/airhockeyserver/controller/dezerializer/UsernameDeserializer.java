package nu.borjessons.airhockeyserver.controller.dezerializer;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import nu.borjessons.airhockeyserver.model.Username;

public class UsernameDeserializer extends ValueDeserializer<Username> {
  @Override
  public Username deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
    return new Username(jsonParser.getString());
  }
}

package nu.borjessons.airhockeyserver.controller.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import nu.borjessons.airhockeyserver.model.Username;

public class UsernameSerializer extends ValueSerializer<Username> {
  @Override
  public void serialize(Username username, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
    jsonGenerator.writeString(username.toString());
  }
}

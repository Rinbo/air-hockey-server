package nu.borjessons.airhockeyserver.controller.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import nu.borjessons.airhockeyserver.model.Username;

public class UsernameSerializer extends JsonSerializer<Username> {
  @Override
  public void serialize(Username username, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeString(username.toString());
  }
}

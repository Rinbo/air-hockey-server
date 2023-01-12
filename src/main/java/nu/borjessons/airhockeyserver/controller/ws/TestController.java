package nu.borjessons.airhockeyserver.controller.ws;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import nu.borjessons.airhockeyserver.model.Message;

@Controller
public class TestController {
  private static final Logger logger = LoggerFactory.getLogger(TestController.class);

  @MessageMapping("/send-message")
  @SendTo("/topic/public")
  public Message sendMessage(Message message) {
    logger.info("Received message: {}", message);
    return new Message(message.username(), message.message(), Instant.now().toString());
  }
}

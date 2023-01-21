package nu.borjessons.airhockeyserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Configuration
public class WebsocketHandler extends TextWebSocketHandler {
  private static final Logger logger = LoggerFactory.getLogger(WebsocketHandler.class);

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) {
    logger.info("========== handleTextMessage in WebsocketHandler was called ==========");
  }
}

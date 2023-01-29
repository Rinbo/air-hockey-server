package nu.borjessons.airhockeyserver.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

@Component
public class WebsocketEventListener {
  private static final Logger logger = LoggerFactory.getLogger(WebsocketEventListener.class);

  @EventListener
  public void handleConnectedEvent(SessionConnectedEvent event) {
    logger.info("connect event: {}", event);
  }
}

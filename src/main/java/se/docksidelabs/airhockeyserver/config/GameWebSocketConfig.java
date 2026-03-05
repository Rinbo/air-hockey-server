package se.docksidelabs.airhockeyserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import se.docksidelabs.airhockeyserver.websocket.GameWebSocketHandler;

/**
 * Registers the raw WebSocket endpoint for the high-frequency game board-state
 * channel.
 * This runs alongside the existing STOMP/SockJS configuration.
 */
@Configuration
@EnableWebSocket
public class GameWebSocketConfig implements WebSocketConfigurer {
    private final GameWebSocketHandler gameWebSocketHandler;
    private final String[] allowedOrigins;

    public GameWebSocketConfig(
            GameWebSocketHandler gameWebSocketHandler,
            @Value("${cors.allowed-origins}") String allowedOrigins) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game/**")
                .setAllowedOrigins(allowedOrigins);
    }
}

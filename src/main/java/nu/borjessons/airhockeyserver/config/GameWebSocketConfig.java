package nu.borjessons.airhockeyserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import nu.borjessons.airhockeyserver.websocket.GameWebSocketHandler;

/**
 * Registers the raw WebSocket endpoint for the high-frequency game board-state
 * channel.
 * This runs alongside the existing STOMP/SockJS configuration.
 */
@Configuration
@EnableWebSocket
public class GameWebSocketConfig implements WebSocketConfigurer {
    private final GameWebSocketHandler gameWebSocketHandler;

    public GameWebSocketConfig(GameWebSocketHandler gameWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game/**")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:4173",
                        "https://localhost:5173",
                        "https://rinbo.github.io");
    }
}

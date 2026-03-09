package se.docksidelabs.airhockeyserver.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import se.docksidelabs.airhockeyserver.config.JwtValidator;
import se.docksidelabs.airhockeyserver.game.BroadcastState;
import se.docksidelabs.airhockeyserver.game.properties.Position;
import se.docksidelabs.airhockeyserver.model.Agency;
import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.service.api.GameService;

/**
 * Raw WebSocket handler for the high-frequency game board-state channel.
 * Replaces STOMP for the board-state topic to reduce protocol overhead.
 *
 * <p>
 * Binary protocol:
 * <ul>
 * <li>Server → Client (64 bytes): 8 × Float64 [opponentX, opponentY, puckX,
 * puckY, puckSpeedX, puckSpeedY, remainingSeconds, collisionEvent]</li>
 * <li>Client → Server (16 bytes): 2 × Float64 [handleX, handleY]</li>
 * </ul>
 *
 * <p>
 * Connection URL: /ws/game/{gameId}/{agency}?token={jwt} where agency is
 * "player-1" or "player-2"
 */
@Component
public class GameWebSocketHandler extends BinaryWebSocketHandler {
  private static final int BROADCAST_STATE_BYTES = 8 * Double.BYTES; // 64 bytes
  private static final int HANDLE_UPDATE_BYTES = 2 * Double.BYTES; // 16 bytes
  private static final int SEND_BUFFER_LIMIT = 16 * 1024; // 16 KB — ~250 state frames
  private static final int SEND_TIME_LIMIT_MS = 5_000; // 5 seconds
  private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);
  /**
   * Key: "gameId:agency" (e.g. "abc123:PLAYER_1"), Value: WebSocketSession
   */
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final GameService gameService;
  private final JwtValidator jwtValidator;

  public GameWebSocketHandler(GameService gameService, JwtValidator jwtValidator) {
    this.gameService = gameService;
    this.jwtValidator = jwtValidator;
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
    String gameId = (String) session.getAttributes().get("gameId");
    Agency agency = (Agency) session.getAttributes().get("agency");
    if (gameId != null && agency != null) {
      sessions.remove(sessionKey(gameId, agency));
      logger.info("Game WS disconnected: {} as {}", gameId, agency);
    }
  }

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) {
    // Validate JWT from query parameter
    String token = extractQueryParam(session, "token");
    if (token == null || jwtValidator.validate(token).isEmpty()) {
      logger.warn("Game WS rejected: invalid or missing token");
      closeQuietly(session);
      return;
    }

    String[] parts = parseUri(session);
    if (parts == null) {
      closeQuietly(session);
      return;
    }

    String gameId = parts[0];
    Agency agency = parseAgency(parts[1]);
    if (agency == null) {
      closeQuietly(session);
      return;
    }

    String key = sessionKey(gameId, agency);
    // Wrap session to prevent sendMessage() from blocking the game loop thread
    // when the client's TCP send buffer is full. Slow clients will be
    // disconnected after SEND_TIME_LIMIT_MS rather than stalling the server.
    WebSocketSession concurrentSession = new ConcurrentWebSocketSessionDecorator(
        session, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT);
    sessions.put(key, concurrentSession);

    session.getAttributes().put("gameId", gameId);
    session.getAttributes().put("agency", agency);

    logger.info("Game WS connected: {} as {}", gameId, agency);
  }

  /**
   * Called from the game loop to broadcast board state to a specific player.
   */
  public void sendBoardState(GameId gameId, Agency agency, BroadcastState state) {
    String key = sessionKey(gameId.toString(), agency);
    WebSocketSession session = sessions.get(key);
    if (session == null || !session.isOpen())
      return;

    try {
      ByteBuffer buffer = ByteBuffer.allocate(BROADCAST_STATE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putDouble(state.getOpponent().getX());
      buffer.putDouble(state.getOpponent().getY());
      buffer.putDouble(state.getPuck().getX());
      buffer.putDouble(state.getPuck().getY());
      buffer.putDouble(state.getPuckSpeed().getX());
      buffer.putDouble(state.getPuckSpeed().getY());
      buffer.putDouble(state.getRemainingSeconds());
      buffer.putDouble(state.getCollisionEvent());
      buffer.flip();

      session.sendMessage(new BinaryMessage(buffer));
    } catch (IOException e) {
      logger.warn("Failed to send board state to {} {}: {}", gameId, agency, e.getMessage());
    } catch (Exception e) {
      // ConcurrentWebSocketSessionDecorator throws SessionLimitExceededException
      // when the client is too slow — disconnect rather than block the game loop
      logger.warn("Slow client disconnected {} {}: {}", gameId, agency, e.getMessage());
      sessions.remove(key);
    }
  }

  @Override
  protected void handleBinaryMessage(@NonNull WebSocketSession session, BinaryMessage message) {
    ByteBuffer payload = message.getPayload();
    if (payload.remaining() < HANDLE_UPDATE_BYTES)
      return;

    payload.order(ByteOrder.LITTLE_ENDIAN);
    double x = payload.getDouble();
    double y = payload.getDouble();

    String gameId = (String) session.getAttributes().get("gameId");
    Agency agency = (Agency) session.getAttributes().get("agency");
    if (gameId == null || agency == null)
      return;

    Position position = new Position(x, y);
    gameService.getGameStore(new GameId(gameId))
        .ifPresent(store -> store.updateHandle(position, agency));
  }

  private void closeQuietly(WebSocketSession session) {
    try {
      session.close();
    } catch (IOException e) {
      logger.warn("Error closing session: {}", e.getMessage());
    }
  }

  private Agency parseAgency(String agencyStr) {
    return switch (agencyStr) {
      case "player-1" -> Agency.PLAYER_1;
      case "player-2" -> Agency.PLAYER_2;
      default -> {
        logger.warn("Invalid agency: {}", agencyStr);
        yield null;
      }
    };
  }

  private String[] parseUri(WebSocketSession session) {
    // URI format: /ws/game/{gameId}/{agency}
    String path = session.getUri() != null ? session.getUri().getPath() : "";
    String[] segments = path.split("/");
    // Expected: ["", "ws", "game", "{gameId}", "{agency}"]
    if (segments.length < 5) {
      logger.warn("Invalid game WS URI: {}", path);
      return null;
    }
    return new String[] { segments[3], segments[4] };
  }

  private String sessionKey(String gameId, Agency agency) {
    return gameId + ":" + agency;
  }

  private String extractQueryParam(WebSocketSession session, String name) {
    if (session.getUri() == null)
      return null;
    String query = session.getUri().getQuery();
    if (query == null)
      return null;
    for (String param : query.split("&")) {
      String[] kv = param.split("=", 2);
      if (kv.length == 2 && name.equals(kv[0])) {
        return kv[1];
      }
    }
    return null;
  }
}

package se.docksidelabs.airhockeyserver.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import se.docksidelabs.airhockeyserver.game.BroadcastState;
import se.docksidelabs.airhockeyserver.game.properties.Position;
import se.docksidelabs.airhockeyserver.model.Agency;
import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.service.api.GameService;

/**
 * Dual-protocol {@link BoardTransport} for receiving player input and
 * broadcasting board state via the Go WebTransport sidecar.
 *
 * <p>Accepts <strong>inbound</strong> datagrams over <strong>UDP</strong> (registration
 * and player input from the sidecar) and sends <strong>outbound</strong> board state
 * over a persistent <strong>TCP</strong> connection.
 *
 * <p>This split design works around Docker Desktop on macOS, which reliably
 * forwards <em>inbound</em> UDP (host→container) but fails to route UDP
 * <em>responses</em> back from the container to the host. TCP is used for
 * the outbound path because it works perfectly through Docker's port mapping.
 *
 * <h3>Wire format</h3>
 * <pre>
 *   Sidecar → Java (UDP registration): [sessionId:2][0x01][gameId UTF-8][0x00][agency:1]
 *   Sidecar → Java (UDP input):        [sessionId:2][handleX:8][handleY:8]  = 18 bytes
 *   Java → Sidecar (TCP state):        [length:2 LE][sessionId:2][6×Float64 LE]  = 2+50 bytes
 * </pre>
 */
public class UdpBoardTransport implements BoardTransport, SmartLifecycle {

  private static final Logger logger = LoggerFactory.getLogger(UdpBoardTransport.class);

  private static final int SESSION_ID_BYTES = 2;
  private static final int INPUT_PAYLOAD_BYTES = 2 * Double.BYTES;    // 16 bytes
  private static final int STATE_PAYLOAD_BYTES = 6 * Double.BYTES;    // 48 bytes
  private static final int MAX_PACKET_SIZE = 256;                     // generous buffer
  private static final byte REGISTER_FLAG = 0x01;

  private final int port;
  private final GameService gameService;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  /**
   * Maps "gameId:agency" → session ID assigned by the sidecar.
   */
  private final Map<String, Short> gameAgencyToSession = new ConcurrentHashMap<>();

  /**
   * Reverse map: session ID → "gameId:agency" key for O(1) input routing.
   */
  private final Map<Short, String> sessionToGameAgency = new ConcurrentHashMap<>();

  private volatile DatagramSocket udpSocket;
  private volatile ServerSocket tcpServerSocket;
  private volatile OutputStream tcpOut;
  private final Object tcpWriteLock = new Object();
  private volatile boolean running;

  public UdpBoardTransport(int port, GameService gameService) {
    this.port = port;
    this.gameService = gameService;
  }

  // ── SmartLifecycle ──────────────────────────────────────────────

  @Override
  public void start() {
    try {
      // UDP socket for inbound registration + input from sidecar
      udpSocket = new DatagramSocket(port);
      running = true;
      executor.submit(this::udpReceiveLoop);
      logger.info("UDP transport listening on port {}", port);

      // TCP server socket for outbound board state to sidecar
      int tcpPort = port + 1; // TCP on port+1 (e.g. 9001)
      tcpServerSocket = new ServerSocket(tcpPort);
      executor.submit(this::tcpAcceptLoop);
      logger.info("TCP transport listening on port {} (for board state responses)", tcpPort);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to bind transport sockets on port " + port, e);
    }
  }

  @Override
  public void stop() {
    running = false;
    if (udpSocket != null && !udpSocket.isClosed()) {
      udpSocket.close();
    }
    try {
      if (tcpServerSocket != null && !tcpServerSocket.isClosed()) {
        tcpServerSocket.close();
      }
    } catch (IOException e) {
      logger.warn("Error closing TCP server socket: {}", e.getMessage());
    }
    executor.close();
    logger.info("Board transport stopped");
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  // ── TCP accept loop ────────────────────────────────────────────

  private void tcpAcceptLoop() {
    while (running) {
      try {
        Socket client = tcpServerSocket.accept();
        client.setTcpNoDelay(true);
        synchronized (tcpWriteLock) {
          // Close any previous connection
          if (tcpOut != null) {
            try { tcpOut.close(); } catch (IOException ignored) {}
          }
          tcpOut = client.getOutputStream();
        }
        logger.info("TCP sidecar connected from {}", client.getRemoteSocketAddress());

        // Read loop to detect disconnection (sidecar doesn't send data over TCP)
        executor.submit(() -> {
          try {
            InputStream in = client.getInputStream();
            while (in.read() != -1) { /* drain */ }
          } catch (IOException ignored) {}
          synchronized (tcpWriteLock) {
            if (tcpOut != null) {
              try { tcpOut.close(); } catch (IOException ignored) {}
              tcpOut = null;
            }
          }
          logger.info("TCP sidecar disconnected");
        });
      } catch (IOException e) {
        if (running) {
          logger.warn("TCP accept error: {}", e.getMessage());
        }
      }
    }
  }

  // ── BoardTransport ──────────────────────────────────────────────

  private static final ThreadLocal<ByteBuffer> SEND_BUFFER = ThreadLocal.withInitial(
      () -> ByteBuffer.allocate(2 + SESSION_ID_BYTES + STATE_PAYLOAD_BYTES)
          .order(ByteOrder.LITTLE_ENDIAN));

  @Override
  public void sendBoardState(GameId gameId, Agency agency, BroadcastState state) {
    String key = sessionKey(gameId.toString(), agency);
    Short sessionId = gameAgencyToSession.get(key);
    if (sessionId == null) return;

    OutputStream out = tcpOut;
    if (out == null) return;

    try {
      ByteBuffer buffer = SEND_BUFFER.get();
      buffer.clear();

      // Length prefix (little-endian u16): payload is sessionId(2) + 6*Float64(48) = 50
      int payloadLen = SESSION_ID_BYTES + STATE_PAYLOAD_BYTES;
      buffer.putShort((short) payloadLen);

      // Payload
      buffer.putShort(sessionId);
      buffer.putDouble(state.getOpponent().getX());
      buffer.putDouble(state.getOpponent().getY());
      buffer.putDouble(state.getPuck().getX());
      buffer.putDouble(state.getPuck().getY());
      buffer.putDouble(state.getRemainingSeconds());
      buffer.putDouble(state.getCollisionEvent());
      buffer.flip();

      byte[] data = new byte[buffer.remaining()];
      buffer.get(data);

      synchronized (tcpWriteLock) {
        if (tcpOut != null) {
          tcpOut.write(data);
          tcpOut.flush();
        }
      }
    } catch (IOException e) {
      logger.warn("Failed to send board state via TCP to {} {}: {}", gameId, agency, e.getMessage());
      synchronized (tcpWriteLock) {
        tcpOut = null; // Mark as disconnected
      }
    }
  }

  // ── UDP Receive Loop ────────────────────────────────────────────

  private void udpReceiveLoop() {
    byte[] buf = new byte[MAX_PACKET_SIZE];

    while (running && !udpSocket.isClosed()) {
      try {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        udpSocket.receive(packet);

        // Dispatch on a virtual thread for concurrent processing
        byte[] packetData = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), packetData, 0, packet.getLength());

        executor.submit(() -> handlePacket(packetData));
      } catch (IOException e) {
        if (running) {
          logger.warn("UDP receive error: {}", e.getMessage());
        }
      }
    }
  }

  private void handlePacket(byte[] data) {
    if (data.length < SESSION_ID_BYTES + 1) return;

    ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    short sessionId = buffer.getShort();

    int remaining = data.length - SESSION_ID_BYTES;

    // Input packets are exactly 16 bytes of Float64 handle position (the
    // 60Hz hot path). Check size first to avoid false positives — Float64
    // data can coincidentally start with 0x01 (the REGISTER_FLAG byte).
    if (remaining == INPUT_PAYLOAD_BYTES) {
      handleInput(sessionId, buffer);
      return;
    }

    // Registration packet: [0x01][gameId UTF-8][0x00][agency:1] (min 4 bytes)
    if (remaining >= 4 && data[SESSION_ID_BYTES] == REGISTER_FLAG) {
      handleRegistration(sessionId, data);
    }
  }

  private void handleRegistration(short sessionId, byte[] data) {
    // New format from sidecar: [sessionId:2][0x01][gameId UTF-8][0x00 separator][agency:1][0x00 separator][userId UTF-8]
    // The sidecar appends [0x00][userId] after validating the JWT.
    int payloadStart = SESSION_ID_BYTES + 1; // skip sessionId + register flag

    // Find first 0x00 separator (after gameId)
    int firstSeparator = -1;
    for (int i = payloadStart; i < data.length; i++) {
      if (data[i] == 0x00) {
        firstSeparator = i;
        break;
      }
    }

    if (firstSeparator < 0 || firstSeparator + 1 >= data.length) {
      logger.warn("Invalid registration packet: no first separator found");
      return;
    }

    String gameId = new String(data, payloadStart, firstSeparator - payloadStart, StandardCharsets.UTF_8);
    byte agencyByte = data[firstSeparator + 1];
    Agency agency = switch (agencyByte) {
      case 0x01 -> Agency.PLAYER_1;
      case 0x02 -> Agency.PLAYER_2;
      default -> null;
    };

    if (agency == null) {
      logger.warn("Invalid agency byte in registration: 0x{}", Integer.toHexString(agencyByte));
      return;
    }

    // Find second 0x00 separator (after agency byte) — userId follows
    int secondSeparator = firstSeparator + 2; // agency is 1 byte, separator should be right after
    if (secondSeparator >= data.length || data[secondSeparator] != 0x00) {
      logger.warn("Registration rejected: missing userId (unauthenticated packet from session {})", sessionId);
      return;
    }

    if (secondSeparator + 1 >= data.length) {
      logger.warn("Registration rejected: empty userId from session {}", sessionId);
      return;
    }

    String userId = new String(data, secondSeparator + 1, data.length - secondSeparator - 1, StandardCharsets.UTF_8);

    // Validate that this userId is a valid player with the claimed agency in this game
    var gameStore = gameService.getGameStore(new GameId(gameId));
    if (gameStore.isEmpty()) {
      logger.warn("Registration rejected: game {} not found (session {}, userId={})", gameId, sessionId, userId);
      return;
    }

    var players = gameStore.get().getPlayers();
    boolean authorized = players.stream()
        .anyMatch(player -> player.getAgency() == agency
            && userId.equals(player.getGatewayUserId()));

    if (!authorized) {
      logger.warn("Registration rejected: userId {} is not {} in game {} (session {})",
          userId, agency, gameId, sessionId);
      return;
    }

    String key = sessionKey(gameId, agency);
    gameAgencyToSession.put(key, sessionId);
    sessionToGameAgency.put(sessionId, key);
    logger.info("UDP session registered: session={} game={} agency={} userId={}", sessionId, gameId, agency, userId);
  }

  private void handleInput(short sessionId, ByteBuffer buffer) {
    double x = buffer.getDouble();
    double y = buffer.getDouble();

    // O(1) reverse-lookup via sessionToGameAgency map
    String key = sessionToGameAgency.get(sessionId);
    if (key == null) return;

    String[] parts = key.split(":");
    String gameId = parts[0];
    Agency agency = Agency.valueOf(parts[1]);
    Position position = new Position(x, y);

    gameService.getGameStore(new GameId(gameId))
        .ifPresent(store -> store.updateHandle(position, agency));
  }

  /**
   * Removes session mappings for a game+agency (called on disconnect).
   */
  public void deregister(GameId gameId, Agency agency) {
    String key = sessionKey(gameId.toString(), agency);
    Short sessionId = gameAgencyToSession.remove(key);
    if (sessionId != null) {
      sessionToGameAgency.remove(sessionId);
    }
  }

  private static String sessionKey(String gameId, Agency agency) {
    return gameId + ":" + agency;
  }
}

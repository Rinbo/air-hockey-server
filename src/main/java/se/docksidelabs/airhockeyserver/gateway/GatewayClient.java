package se.docksidelabs.airhockeyserver.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/**
 * HTTP client for service-to-service calls from the game server to the gateway.
 * Uses X-Service-Key authentication.
 */
@Component
public class GatewayClient {
  private static final Logger logger = LoggerFactory.getLogger(GatewayClient.class);

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String gatewayUrl;
  private final String serviceKey;

  public GatewayClient(
      ObjectMapper objectMapper,
      @Value("${gateway.url:}") String gatewayUrl,
      @Value("${gateway.servicekey:}") String serviceKey) {
    this.objectMapper = objectMapper;
    this.gatewayUrl = gatewayUrl;
    this.serviceKey = serviceKey;
    this.httpClient = HttpClient.newHttpClient();

    if (gatewayUrl.isBlank() || serviceKey.isBlank()) {
      throw new IllegalStateException("Gateway client not configured");
    }
  }

  /**
   * Report this server's capacity to the gateway so it can make
   * scheduling decisions. Fire-and-forget — logs errors but never throws.
   */
  public void sendHeartbeat(String machineId, int activeGames, int maxGames, String region) {
    try {
      String body = objectMapper.writeValueAsString(Map.of(
          "machineId", machineId,
          "activeGames", activeGames,
          "maxGames", maxGames,
          "region", region));

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(gatewayUrl + "/api/servers/heartbeat"))
          .header("Content-Type", "application/json")
          .header("X-Service-Key", serviceKey)
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() > 299) {
        logger.warn("Gateway returned {} for heartbeat: {}", response.statusCode(), response.body());
      }
    } catch (Exception e) {
      logger.warn("Failed to send heartbeat to gateway", e);
    }
  }

  /**
   * Report that a game was played by the given user.
   * Fire-and-forget — logs errors but never throws.
   */
  public void reportGamePlayed(String userId) {
    Objects.requireNonNull(userId, "userId must not be null");

    try {
      String body = objectMapper.writeValueAsString(Map.of("userId", userId));

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(gatewayUrl + "/api/user/games-played"))
          .header("Content-Type", "application/json")
          .header("X-Service-Key", serviceKey)
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() > 299) {
        logger.warn("Gateway returned {} for games-played: {}", response.statusCode(), response.body());
      }
    } catch (Exception e) {
      logger.warn("Failed to report game played for user {}", userId, e);
    }
  }
}

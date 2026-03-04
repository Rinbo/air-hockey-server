package se.docksidelabs.airhockeyserver.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HTTP client for service-to-service calls from the game server to the gateway.
 * Uses X-Service-Key authentication.
 */
@Component
public class GatewayClient {
  private static final Logger logger = LoggerFactory.getLogger(GatewayClient.class);

  private final HttpClient httpClient;
  private final String gatewayUrl;
  private final String serviceKey;

  public GatewayClient(
      @Value("${gateway.url:}") String gatewayUrl,
      @Value("${gateway.servicekey:}") String serviceKey) {
    this.gatewayUrl = gatewayUrl;
    this.serviceKey = serviceKey;
    this.httpClient = HttpClient.newHttpClient();

    if (gatewayUrl.isBlank() || serviceKey.isBlank()) {
      throw new IllegalStateException("Gateway client not configured");
    }
  }

  /**
   * Report that a game was played by the given user.
   * Fire-and-forget — logs errors but never throws.
   */
  public void reportGamePlayed(String userId) {
    Objects.requireNonNull(userId, "userId must not be null");

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(gatewayUrl + "/api/user/games-played"))
          .header("Content-Type", "application/json")
          .header("X-Service-Key", serviceKey)
          .POST(HttpRequest.BodyPublishers.ofString("{\"userId\":\"" + userId + "\"}"))
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

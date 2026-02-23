package nu.borjessons.airhockeyserver.utils;

import java.util.Map;
import java.util.Optional;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Username;

public final class HeaderUtils {
  private static final String AGENCY_HEADER = "agency";
  private static final String GAME_ID_HEADER = "gameId";
  private static final String USERNAME_HEADER = "username";

  public static GameId getGameId(SimpMessageHeaderAccessor header) {
    return new GameId(getAttribute(header, GAME_ID_HEADER));
  }

  public static Username getUsername(SimpMessageHeaderAccessor header) {
    return new Username(getAttribute(header, USERNAME_HEADER));
  }

  public static void setGameId(SimpMessageHeaderAccessor header, GameId gameId) {
    setAttribute(header, GAME_ID_HEADER, gameId.toString());
  }

  public static void setUsername(SimpMessageHeaderAccessor header, Username username) {
    setAttribute(header, USERNAME_HEADER, username.toString());
  }

  public static Agency getAgency(SimpMessageHeaderAccessor header) {
    return (Agency) getMap(header).map(map -> map.get(AGENCY_HEADER)).orElse(null);
  }

  public static void setAgency(SimpMessageHeaderAccessor header, Agency agency) {
    getMap(header).ifPresent(map -> map.put(AGENCY_HEADER, agency));
  }

  private static String getAttribute(SimpMessageHeaderAccessor header, String key) {
    return getMap(header).map(map -> (String) map.get(key)).orElse("");
  }

  private static Optional<Map<String, Object>> getMap(SimpMessageHeaderAccessor header) {
    return Optional.ofNullable(header.getSessionAttributes());
  }

  private static void setAttribute(SimpMessageHeaderAccessor header, String key, String value) {
    getMap(header).ifPresent(map -> map.put(key, value));
  }

  private HeaderUtils() {
    throw new IllegalStateException();
  }
}

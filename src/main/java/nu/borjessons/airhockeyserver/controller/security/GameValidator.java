package nu.borjessons.airhockeyserver.controller.security;

import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import nu.borjessons.airhockeyserver.model.AuthRecord;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.GameStore;
import nu.borjessons.airhockeyserver.service.api.GameService;
import nu.borjessons.airhockeyserver.utils.HeaderUtils;

@Component
public class GameValidator {
  private static GameId validateGameId(SimpMessageHeaderAccessor header, String gameIdString) {
    GameId gameId = HeaderUtils.getGameId(header);
    if (!gameIdString.equals(gameId.toString())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not in the correct game");
    return gameId;
  }

  private final GameService gameService;

  public GameValidator(GameService gameService) {
    Objects.requireNonNull(gameService, "gameService must not be null");

    this.gameService = gameService;
  }

  public AuthRecord validateUser(SimpMessageHeaderAccessor header, String gameIdString) {
    GameId validatedGameId = validateGameId(header, gameIdString);
    Username username = HeaderUtils.getUsername(header);

    GameStore gameStore = gameService.getGameStore(validatedGameId).orElseThrow();
    Optional<Player> optional = gameStore.getPlayer(username);
    if (optional.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not belong in this game");

    return new AuthRecord(validatedGameId, username);
  }
}

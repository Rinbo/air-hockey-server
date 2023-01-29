package nu.borjessons.airhockeyserver.service.api;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Username;

public interface CountdownService {
  void handleBothPlayersReady(GameId gameId, Username username);
}

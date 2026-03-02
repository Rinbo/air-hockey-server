package se.docksidelabs.airhockeyserver.service.api;

import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.model.Username;

public interface CountdownService {
  void cancelTimer(GameId gameId);

  void handleBothPlayersReady(GameId gameId, Username username);
}

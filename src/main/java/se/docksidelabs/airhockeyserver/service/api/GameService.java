package se.docksidelabs.airhockeyserver.service.api;

import java.util.Collection;
import java.util.Optional;

import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.model.GameState;
import se.docksidelabs.airhockeyserver.model.Player;
import se.docksidelabs.airhockeyserver.model.Username;
import se.docksidelabs.airhockeyserver.repository.GameStore;

public interface GameService {
  boolean addUserToGame(GameId gameId, Username username);

  void deleteGame(GameId gameId);

  GameState getGameState(GameId gameId);

  Optional<GameStore> getGameStore(GameId gameId);

  Collection<GameStore> getGameStores();

  Optional<Player> getPlayer(GameId gameId, Username username);

  Collection<Player> getPlayers(GameId gameId);

  void handleUserDisconnect(GameId gameId, Username username);

  void removeUser(GameId gameId, Username username);

  void toggleReady(GameId gameId, Username userName);
}

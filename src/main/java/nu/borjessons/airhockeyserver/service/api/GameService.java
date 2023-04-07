package nu.borjessons.airhockeyserver.service.api;

import java.util.Collection;
import java.util.Optional;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.GameStore;

public interface GameService {
  boolean addUserToGame(GameId gameId, Username username);

  void deleteGame(GameId gameId);

  Optional<GameStore> getGameStore(GameId gameId);

  Collection<GameStore> getGameStores();

  Optional<Player> getPlayer(GameId gameId, Username username);

  Collection<Player> getPlayers(GameId gameId);

  void handleUserDisconnect(GameId gameId, Username username);

  void removeUser(GameId gameId, Username username);

  void toggleReady(GameId gameId, Username userName);
}

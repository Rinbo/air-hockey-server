package nu.borjessons.airhockeyserver.service.api;

import java.util.Optional;
import java.util.Set;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;

public interface GameService {
  boolean addUserToGame(GameId gameId, Username username);

  void deleteGame(GameId gameId);

  Optional<Player> getPlayer(GameId gameId, Username username);

  Set<Player> getPlayers(GameId gameId);

  void removeUser(GameId gameId, Username username);
}

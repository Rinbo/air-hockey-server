package nu.borjessons.airhockeyserver.service.api;

import java.util.Set;

import nu.borjessons.airhockeyserver.model.LobbyId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;

public interface GameService {
  boolean addUserToLobby(LobbyId lobbyId, Username username);

  Set<Player> getPlayers(LobbyId lobbyId);
}

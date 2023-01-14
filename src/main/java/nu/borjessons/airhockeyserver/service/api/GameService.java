package nu.borjessons.airhockeyserver.service.api;

import nu.borjessons.airhockeyserver.model.LobbyId;
import nu.borjessons.airhockeyserver.model.Username;

public interface GameService {
  void addUserToLobby(LobbyId lobbyId, Username username);
}

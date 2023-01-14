package nu.borjessons.airhockeyserver.service.api;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import nu.borjessons.airhockeyserver.model.LobbyId;
import nu.borjessons.airhockeyserver.model.Username;

@Service
public class GameServiceImpl implements GameService {
  private final ConcurrentHashMap<LobbyId, Set<Username>> userStore;

  public GameServiceImpl() {
    this.userStore = new ConcurrentHashMap<>();
  }

  @Override
  public void addUserToLobby(LobbyId lobbyId, Username username) {
    userStore.computeIfAbsent(lobbyId, k -> new HashSet<>()).add(username);
  }
}

package nu.borjessons.airhockeyserver.service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import nu.borjessons.airhockeyserver.model.Agent;
import nu.borjessons.airhockeyserver.model.LobbyId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.service.api.GameService;

@Service
public class GameServiceImpl implements GameService {
  private final ConcurrentHashMap<LobbyId, Set<Player>> userStore;

  public GameServiceImpl() {
    this.userStore = new ConcurrentHashMap<>();
  }

  @Override
  public boolean addUserToLobby(LobbyId lobbyId, Username username) {
    Set<Player> set = userStore.computeIfAbsent(lobbyId, k -> new HashSet<>());
    return switch (set.size()) {
      case 0 -> set.add(new Player(username, Agent.PLAYER_1));
      case 1 -> set.add(new Player(username, Agent.PLAYER_2));
      default -> false;
    };
  }

  @Override
  public Set<Player> getPlayers(LobbyId lobbyId) {
    return userStore.get(lobbyId);
  }
}

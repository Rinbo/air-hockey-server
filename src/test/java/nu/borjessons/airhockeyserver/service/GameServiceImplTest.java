package nu.borjessons.airhockeyserver.service;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.model.Agent;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.service.api.GameService;

class GameServiceImplTest {
  private static final GameId GAME_ID = new GameId("gameId");
  private static final Player PLAYER1 = new Player(new Username("Player1"), Agent.PLAYER_1);
  private static final Player PLAYER2 = new Player(new Username("Player2"), Agent.PLAYER_2);

  private static Map<GameId, Set<Player>> createGameStore(GameId gameId, Player... players) {
    Map<GameId, Set<Player>> map = new ConcurrentHashMap<>();
    map.put(gameId, Arrays.stream(players).collect(Collectors.toSet()));
    return map;
  }

  @Test
  void removeUserNonExistentGameIdTest() {
    Map<GameId, Set<Player>> gameStore = createGameStore(GAME_ID, PLAYER1, PLAYER2);
    GameService gameService = new GameServiceImpl(gameStore);

    gameService.removeUser(new GameId("unknown"), PLAYER1.username());

    Assertions.assertEquals(2, gameStore.get(GAME_ID).size());
    Assertions.assertEquals(1, gameStore.size());
  }

  @Test
  void removeUserTest() {
    Map<GameId, Set<Player>> gameStore = createGameStore(GAME_ID, PLAYER1, PLAYER2);
    GameService gameService = new GameServiceImpl(gameStore);

    gameService.removeUser(GAME_ID, PLAYER1.username());
    Assertions.assertEquals(PLAYER2, gameStore.get(GAME_ID).iterator().next());

    gameService.removeUser(GAME_ID, PLAYER2.username());
    Assertions.assertTrue(gameStore.get(GAME_ID).isEmpty());
  }
}

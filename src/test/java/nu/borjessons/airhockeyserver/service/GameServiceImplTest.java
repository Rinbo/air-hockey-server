package nu.borjessons.airhockeyserver.service;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.service.api.GameService;
import nu.borjessons.airhockeyserver.utils.TestUtils;

class GameServiceImplTest {
  private static Map<GameId, Set<Player>> createGameStore(GameId gameId, Player... players) {
    Map<GameId, Set<Player>> map = new ConcurrentHashMap<>();
    map.put(gameId, Arrays.stream(players).collect(Collectors.toSet()));
    return map;
  }

  @Test
  void removeUserNonExistentGameIdTest() {
    Map<GameId, Set<Player>> gameStoreMap = createGameStore(TestUtils.GAME_ID, TestUtils.PLAYER1, TestUtils.PLAYER2);
    GameService gameService = new GameServiceImpl(gameStoreMap);

    gameService.removeUser(new GameId("unknown"), TestUtils.PLAYER1.getUsername());

    Assertions.assertEquals(2, gameStoreMap.get(TestUtils.GAME_ID).size());
    Assertions.assertEquals(1, gameStoreMap.size());
  }

  @Test
  void removeUserTest() {
    Map<GameId, Set<Player>> gameStoreMap = createGameStore(TestUtils.GAME_ID, TestUtils.PLAYER1, TestUtils.PLAYER2);
    GameService gameService = new GameServiceImpl(gameStoreMap);

    gameService.removeUser(TestUtils.GAME_ID, TestUtils.PLAYER1.getUsername());
    Assertions.assertEquals(TestUtils.PLAYER2, gameStoreMap.get(TestUtils.GAME_ID).iterator().next());

    gameService.removeUser(TestUtils.GAME_ID, TestUtils.PLAYER2.getUsername());
    Assertions.assertTrue(gameStoreMap.get(TestUtils.GAME_ID).isEmpty());
  }
}

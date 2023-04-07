package nu.borjessons.airhockeyserver.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.repository.GameStore;
import nu.borjessons.airhockeyserver.service.api.GameService;
import nu.borjessons.airhockeyserver.utils.TestUtils;

class GameServiceImplTest {
  private static Map<GameId, GameStore> createGameStoreMap() {
    Map<GameId, GameStore> map = new ConcurrentHashMap<>();
    map.put(TestUtils.GAME_ID, TestUtils.createGameStore());
    return map;
  }

  @Test
  void removeUserNonExistentGameIdTest() {
    Map<GameId, GameStore> gameStoreMap = createGameStoreMap();
    SimpMessagingTemplate messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
    GameService gameService = new GameServiceImpl(gameStoreMap, messagingTemplate);

    gameService.removeUser(new GameId("unknown"), TestUtils.USER1);

    Assertions.assertEquals(2, gameStoreMap.get(TestUtils.GAME_ID).getPlayers().size());
    Assertions.assertEquals(1, gameStoreMap.size());

    Mockito.verifyNoInteractions(messagingTemplate);
  }

  @Test
  void removeUserTest() {
    Map<GameId, GameStore> gameStoreMap = createGameStoreMap();
    SimpMessagingTemplate messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
    GameService gameService = new GameServiceImpl(gameStoreMap, messagingTemplate);

    gameService.removeUser(TestUtils.GAME_ID, TestUtils.USER1);
    Assertions.assertEquals(TestUtils.PLAYER2, gameStoreMap.get(TestUtils.GAME_ID).getPlayers().iterator().next());

    gameService.removeUser(TestUtils.GAME_ID, TestUtils.PLAYER2.getUsername());
    Assertions.assertTrue(gameStoreMap.get(TestUtils.GAME_ID).getPlayers().isEmpty());
    Mockito.verifyNoInteractions(messagingTemplate);
  }
}

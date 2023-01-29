package nu.borjessons.airhockeyserver.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.model.GameState;
import nu.borjessons.airhockeyserver.utils.TestUtils;

class GameStoreTest {
  @Test
  void addRemoveUserTest() {
    GameStore gameStore = new GameStore(TestUtils.GAME_ID);
    Assertions.assertEquals(0, gameStore.getPlayers().size());

    gameStore.addPlayer(TestUtils.USER1);
    Assertions.assertEquals(1, gameStore.getPlayers().size());

    gameStore.addPlayer(TestUtils.USER2);
    Assertions.assertEquals(2, gameStore.getPlayers().size());

    gameStore.removePlayer(TestUtils.PLAYER2);
    Assertions.assertEquals(1, gameStore.getPlayers().size());
    Assertions.assertEquals(TestUtils.PLAYER1, gameStore.getPlayers().iterator().next());

    gameStore.removePlayer(TestUtils.PLAYER1);
    Assertions.assertEquals(0, gameStore.getPlayers().size());
  }

  @Test
  void gameStateTest() {
    GameStore gameStore = TestUtils.createGameStore();
    Assertions.assertEquals(GameState.LOBBY, gameStore.getGameState());

    gameStore.setGameState(GameState.GAME_RUNNING);
    Assertions.assertEquals(GameState.GAME_RUNNING, gameStore.getGameState());
  }

  @Test
  void getGameId() {
    Assertions.assertEquals(TestUtils.GAME_ID, TestUtils.createGameStore().getGameId());
  }

  @Test
  void maxPlayerTest() {
    GameStore gameStore = TestUtils.createGameStore();
    Assertions.assertThrows(IllegalStateException.class, () -> gameStore.addPlayer(TestUtils.OTHER_USER));
  }

  @Test
  void readinessToggleTest() {
    GameStore gameStore = TestUtils.createGameStore();
    Assertions.assertFalse(gameStore.getPlayer(TestUtils.PLAYER1.getUsername()).orElseThrow().isReady());
    Assertions.assertFalse(gameStore.getPlayer(TestUtils.PLAYER2.getUsername()).orElseThrow().isReady());

    gameStore.togglePlayerReadiness(TestUtils.PLAYER1);
    Assertions.assertTrue(gameStore.getPlayer(TestUtils.PLAYER1.getUsername()).orElseThrow().isReady());
    Assertions.assertFalse(gameStore.getPlayer(TestUtils.PLAYER2.getUsername()).orElseThrow().isReady());

    gameStore.togglePlayerReadiness(TestUtils.PLAYER2);
    Assertions.assertTrue(gameStore.getPlayer(TestUtils.PLAYER1.getUsername()).orElseThrow().isReady());
    Assertions.assertTrue(gameStore.getPlayer(TestUtils.PLAYER2.getUsername()).orElseThrow().isReady());
  }
}

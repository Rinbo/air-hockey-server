package nu.borjessons.airhockeyserver.game;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import nu.borjessons.airhockeyserver.repository.GameStoreConnector;
import nu.borjessons.airhockeyserver.utils.TestUtils;

class GameRunnableTest {

  @Test
  void testDelay() {
    GameStoreConnector gameStoreConnector = Mockito.mock(GameStoreConnector.class);

    Thread thread = new Thread(new GameRunnable(TestUtils.BOARD_STATE, TestUtils.GAME_ID, gameStoreConnector, false));
    thread.start();

    Mockito.verify(gameStoreConnector, Mockito.timeout(500).atLeast(10))
        .broadcast(ArgumentMatchers.any(BroadcastState.class), ArgumentMatchers.any(BroadcastState.class));
  }
}
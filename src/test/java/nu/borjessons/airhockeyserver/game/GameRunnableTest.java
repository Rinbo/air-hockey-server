package nu.borjessons.airhockeyserver.game;

import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import nu.borjessons.airhockeyserver.repository.GameStoreController;
import nu.borjessons.airhockeyserver.utils.TestUtils;

class GameRunnableTest {

  @Test
  void testDelay() {
    GameStoreController gameStoreController = Mockito.mock(GameStoreController.class);

    Thread thread = new Thread(new GameRunnable(TestUtils.BOARD_STATE, TestUtils.GAME_ID, gameStoreController, Executors.newSingleThreadScheduledExecutor()));
    thread.start();

    Mockito.verify(gameStoreController, Mockito.timeout(500).atLeast(10))
        .broadcast(ArgumentMatchers.any(BroadcastState.class), ArgumentMatchers.any(BroadcastState.class));
  }
}
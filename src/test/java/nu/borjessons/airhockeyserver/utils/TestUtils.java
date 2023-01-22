package nu.borjessons.airhockeyserver.utils;

import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.GameStore;

public class TestUtils {
  public static final GameId GAME_ID = new GameId("gameId");
  public static final Player OTHER_PLAYER = new Player(Agency.PLAYER_1, new Username("OtherPlayer"));
  public static final Player PLAYER1 = new Player(Agency.PLAYER_1, new Username("Player1"));
  public static final Player PLAYER2 = new Player(Agency.PLAYER_2, new Username("Player2"));

  public static GameStore createGameStore() {
    GameStore gameStore = new GameStore(GAME_ID);
    gameStore.addPlayer(PLAYER1);
    gameStore.addPlayer(PLAYER2);
    return gameStore;
  }
}

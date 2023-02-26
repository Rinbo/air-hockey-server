package nu.borjessons.airhockeyserver.utils;

import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.model.Player;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.GameStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestUtils {
    public static final GameId GAME_ID = new GameId("gameId");
    public static final Username OTHER_USER = new Username("OtherPlayer");
    public static final Player OTHER_PLAYER = new Player(Agency.PLAYER_1, OTHER_USER);
    public static final Username USER1 = new Username("Player1");
    public static final Player PLAYER1 = new Player(Agency.PLAYER_1, USER1);
    public static final Username USER2 = new Username("Player2");
    public static final Player PLAYER2 = new Player(Agency.PLAYER_2, USER2);

    // @formatter:off
    public static final double ALLOWED_DELTA = 0.0001;
    public static final int BOARD_HEIGHT = 500;
    public static final int BOARD_WIDTH = 200;
    public static final double BOARD_ASPECT_RATIO = (double) BOARD_WIDTH / BOARD_HEIGHT;
    public static final Position HANDLE_POS_REAL = new Position(50, 50);
    public static final Position HANDLE_POS = new Position(HANDLE_POS_REAL.x() / BOARD_WIDTH, HANDLE_POS_REAL.y() / BOARD_HEIGHT);
    public static final Position PUCK_POS_REAL = new Position(25, 75);
    public static final double Y_DIFF_REAL = PUCK_POS_REAL.y() - HANDLE_POS_REAL.y();
    public static final double X_DIFF_REAL = PUCK_POS_REAL.x() - HANDLE_POS_REAL.x();
    public static final double ANGLE_REAL = Math.atan(Y_DIFF_REAL / X_DIFF_REAL);
    public static final Position PUCK_POS = new Position(PUCK_POS_REAL.x() / BOARD_WIDTH, PUCK_POS_REAL.y() / BOARD_HEIGHT);
    public static final double Y_DIFF = PUCK_POS.y() - HANDLE_POS.y();
    public static final double X_DIFF = PUCK_POS.x() - HANDLE_POS.x();
    // @formatter:on

    public static GameStore createGameStore() {
        GameStore gameStore = new GameStore(GAME_ID);
        gameStore.addPlayer(USER1);
        gameStore.addPlayer(USER2);
        return gameStore;
    }

    @Test
    void assertConstantsTest() {
        double hypotenuse = Math.sqrt(Math.pow(TestUtils.X_DIFF_REAL, 2) + Math.pow(TestUtils.X_DIFF_REAL, 2));
        Assertions.assertEquals(TestUtils.X_DIFF_REAL, (TestUtils.PUCK_POS.x() - TestUtils.HANDLE_POS.x()) * TestUtils.BOARD_WIDTH, TestUtils.ALLOWED_DELTA);
        Assertions.assertEquals(TestUtils.Y_DIFF_REAL, (TestUtils.PUCK_POS.y() - TestUtils.HANDLE_POS.y()) * TestUtils.BOARD_HEIGHT, TestUtils.ALLOWED_DELTA);

        Assertions.assertEquals(hypotenuse, Math.abs(TestUtils.X_DIFF_REAL / Math.cos(TestUtils.ANGLE_REAL)), TestUtils.ALLOWED_DELTA);
        Assertions.assertEquals(hypotenuse, Math.abs(TestUtils.Y_DIFF_REAL / Math.sin(TestUtils.ANGLE_REAL)), TestUtils.ALLOWED_DELTA);
    }
}

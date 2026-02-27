package nu.borjessons.airhockeyserver.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameState;
import nu.borjessons.airhockeyserver.utils.TestUtils;

/**
 * Tests GameStore.updateHandle() — the method that processes player input.
 * This is where Player 2's coordinates get mirrored. If this logic breaks,
 * handle input is either silently dropped or applied to the wrong position.
 */
@DisplayName("GameStore.updateHandle")
class GameStoreUpdateHandleTest {

    @Test
    @DisplayName("updateHandle is a no-op when game is in LOBBY state")
    void noOpInLobbyState() {
        GameStore store = TestUtils.createGameStore();
        // Store starts in LOBBY state
        assertEquals(GameState.LOBBY, store.getGameState());

        // This should silently return without error
        store.updateHandle(new Position(0.3, 0.3), Agency.PLAYER_1);
    }

    @Test
    @DisplayName("updateHandle throws on null position")
    void throwsOnNullPosition() {
        GameStore store = TestUtils.createGameStore();
        assertThrows(NullPointerException.class, () -> store.updateHandle(null, Agency.PLAYER_1));
    }

    @Test
    @DisplayName("updateHandle throws on null agency")
    void throwsOnNullAgency() {
        GameStore store = TestUtils.createGameStore();
        assertThrows(NullPointerException.class, () -> store.updateHandle(new Position(0.5, 0.5), null));
    }
}

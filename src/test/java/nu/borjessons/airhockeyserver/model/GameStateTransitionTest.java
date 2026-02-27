package nu.borjessons.airhockeyserver.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Exhaustive tests for the GameState state machine.
 * If a new state is added or transition rules change, these tests MUST be
 * updated.
 * This guards against accidentally allowing invalid transitions (e.g.
 * restarting
 * a game from CREATOR_LEFT) or blocking valid ones.
 */
@DisplayName("GameState Transitions")
class GameStateTransitionTest {

    @Nested
    @DisplayName("From LOBBY")
    class FromLobby {

        @Test
        @DisplayName("LOBBY → GAME_RUNNING is valid")
        void lobbyToGameRunning() {
            assertTrue(GameState.LOBBY.isValidNextState(GameState.GAME_RUNNING));
        }

        @Test
        @DisplayName("LOBBY → CREATOR_LEFT is valid")
        void lobbyToCreatorLeft() {
            assertTrue(GameState.LOBBY.isValidNextState(GameState.CREATOR_LEFT));
        }

        @Test
        @DisplayName("LOBBY → LOBBY is invalid")
        void lobbyToLobby() {
            assertFalse(GameState.LOBBY.isValidNextState(GameState.LOBBY));
        }

        @Test
        @DisplayName("null input throws NullPointerException")
        void lobbyNullThrows() {
            assertThrows(NullPointerException.class, () -> GameState.LOBBY.isValidNextState(null));
        }
    }

    @Nested
    @DisplayName("From GAME_RUNNING")
    class FromGameRunning {

        @Test
        @DisplayName("GAME_RUNNING → LOBBY is valid (game ends normally)")
        void gameRunningToLobby() {
            assertTrue(GameState.GAME_RUNNING.isValidNextState(GameState.LOBBY));
        }

        @Test
        @DisplayName("GAME_RUNNING → GAME_RUNNING is valid (restart)")
        void gameRunningToGameRunning() {
            assertTrue(GameState.GAME_RUNNING.isValidNextState(GameState.GAME_RUNNING));
        }

        @Test
        @DisplayName("GAME_RUNNING → CREATOR_LEFT is invalid")
        void gameRunningToCreatorLeft() {
            assertFalse(GameState.GAME_RUNNING.isValidNextState(GameState.CREATOR_LEFT));
        }

        @Test
        @DisplayName("null input throws NullPointerException")
        void gameRunningNullThrows() {
            assertThrows(NullPointerException.class, () -> GameState.GAME_RUNNING.isValidNextState(null));
        }
    }

    @Nested
    @DisplayName("From CREATOR_LEFT")
    class FromCreatorLeft {

        @Test
        @DisplayName("CREATOR_LEFT → anything is invalid (terminal state)")
        void creatorLeftIsTerminal() {
            for (GameState state : GameState.values()) {
                assertFalse(GameState.CREATOR_LEFT.isValidNextState(state),
                        "CREATOR_LEFT should be a terminal state; transition to " + state + " should be invalid");
            }
        }

        @Test
        @DisplayName("null input throws NullPointerException")
        void creatorLeftNullThrows() {
            assertThrows(NullPointerException.class, () -> GameState.CREATOR_LEFT.isValidNextState(null));
        }
    }
}

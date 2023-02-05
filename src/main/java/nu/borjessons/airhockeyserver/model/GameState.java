package nu.borjessons.airhockeyserver.model;

import java.util.Objects;

public enum GameState {
  CREATOR_LEFT {
    @Override
    public boolean isValidNextState(GameState newState) {
      Objects.requireNonNull(newState, NEW_STATE);

      return false;
    }
  },

  GAME_RUNNING {
    @Override
    public boolean isValidNextState(GameState newState) {
      Objects.requireNonNull(newState, NEW_STATE);

      return switch (newState) {
        case CREATOR_LEFT, LOBBY -> true;
        case GAME_RUNNING -> false;
      };
    }
  },

  LOBBY {
    @Override
    public boolean isValidNextState(GameState newState) {
      Objects.requireNonNull(newState, NEW_STATE);

      return switch (newState) {
        case CREATOR_LEFT, GAME_RUNNING -> true;
        case LOBBY -> false;
      };
    }
  };

  private static final String NEW_STATE = "gameState must not be null";

  public abstract boolean isValidNextState(GameState gameState);
}

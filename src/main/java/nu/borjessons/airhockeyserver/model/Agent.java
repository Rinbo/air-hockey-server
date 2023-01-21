package nu.borjessons.airhockeyserver.model;

public enum Agent {
  GAME_ADMIN("Game Admin"),
  PLAYER_1("Player 1"),
  PLAYER_2("Player 2");

  private final String string;

  Agent(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return string;
  }
}

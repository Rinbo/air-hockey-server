package nu.borjessons.airhockeyserver.model;

public enum Agent {
  GAME_BOT("GameBot"),
  PLAYER_1("Player_1"),
  PLAYER_2("Player_2");

  private final String string;

  Agent(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return string;
  }
}

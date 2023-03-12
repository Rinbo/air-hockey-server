package nu.borjessons.airhockeyserver.game;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;
import nu.borjessons.airhockeyserver.game.properties.GameConstants;

public record BoardState(Puck puck, Handle playerOne, Handle playerTwo) {
  public void resetObjects() {
    puck.setPosition(GameConstants.PUCK_START_P1);
    puck.setSpeed(GameConstants.ZERO_SPEED);
    playerOne.setPosition(GameConstants.HANDLE_START_P1);
    playerTwo.setPosition(GameConstants.HANDLE_START_P2);
  }
}

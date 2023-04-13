package nu.borjessons.airhockeyserver.game;

import java.util.concurrent.atomic.AtomicInteger;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;
import nu.borjessons.airhockeyserver.game.properties.GameConstants;

public record BoardState(Puck puck, Handle playerOne, Handle playerTwo, AtomicInteger atomicInteger) {
  public BoardState(Puck puck, Handle playerOne, Handle playerTwo) {
    this(puck, playerOne, playerTwo, new AtomicInteger());
  }

  public void resetObjects() {
    puck.setPosition(atomicInteger.incrementAndGet() % 2 == 0 ? GameConstants.PUCK_START_P1 : GameConstants.PUCK_START_P2);
    puck.setSpeed(GameConstants.ZERO_SPEED);
    playerOne.setPosition(GameConstants.HANDLE_START_P1);
    playerTwo.setPosition(GameConstants.HANDLE_START_P2);
  }
}

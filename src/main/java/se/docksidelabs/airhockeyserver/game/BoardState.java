package se.docksidelabs.airhockeyserver.game;

import java.util.concurrent.atomic.AtomicInteger;

import se.docksidelabs.airhockeyserver.game.objects.Handle;
import se.docksidelabs.airhockeyserver.game.objects.Puck;
import se.docksidelabs.airhockeyserver.game.properties.GameConstants;

/**
 * Holds the mutable game objects that make up the board.
 *
 * <p>The {@code roundCounter} alternates the puck's starting position
 * between Player 1's and Player 2's side after each reset, ensuring
 * fair serve distribution.
 */
public record BoardState(Puck puck, Handle playerOne, Handle playerTwo, AtomicInteger roundCounter) {

  public BoardState(Puck puck, Handle playerOne, Handle playerTwo) {
    this(puck, playerOne, playerTwo, new AtomicInteger());
  }

  public void resetObjects() {
    boolean servesToPlayerOne = roundCounter.incrementAndGet() % 2 == 0;
    puck.setPosition(servesToPlayerOne ? GameConstants.PUCK_START_P1 : GameConstants.PUCK_START_P2);
    puck.setSpeed(GameConstants.ZERO_SPEED);
    playerOne.forcePosition(GameConstants.HANDLE_START_P1);
    playerTwo.forcePosition(GameConstants.HANDLE_START_P2);
  }
}

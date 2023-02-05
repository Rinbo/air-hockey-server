package nu.borjessons.airhockeyserver.engine;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.model.GameId;

class GameRunnable implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(GameRunnable.class);

  private final AtomicReference<BoardState> atomicReference;
  private final GameId gameId;
  private final SimpMessagingTemplate messagingTemplate;

  public GameRunnable(AtomicReference<BoardState> atomicReference, GameId gameId, SimpMessagingTemplate messagingTemplate) {
    Objects.requireNonNull(atomicReference, "atomicReference must not be null");
    Objects.requireNonNull(gameId, "gameId must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");

    this.atomicReference = atomicReference;
    this.messagingTemplate = messagingTemplate;
    this.gameId = gameId;
  }

  private static BroadcastState createBroadcastState(Position opponentPosition, Position puckPosition) {
    return new BroadcastState(opponentPosition, puckPosition);
  }

  @Override
  public void run() {
    logger.info("Starting game loop: {}", gameId);

    String playerOneTopic = String.format("/topic/game/%s/board-state/player-1", gameId);
    String playerTwoTopic = String.format("/topic/game/%s/board-state/player-2", gameId);

    while (!Thread.currentThread().isInterrupted()) {
      try {
        broadcast(playerOneTopic, playerTwoTopic);
        TimeUnit.SECONDS.sleep(1 / GameConstants.FRAME_RATE);
      } catch (InterruptedException e) {
        logger.info("Interrupt called on gameThread: {}", gameId);
        Thread.currentThread().interrupt();
      }
    }

    logger.info("exiting game loop: {}", gameId);
  }

  private void broadcast(String playerOneTopic, String playerTwoTopic) {
    BoardState boardState = atomicReference.get();
    Position puckPosition = boardState.puck().position();
    messagingTemplate.convertAndSend(playerOneTopic,
        createBroadcastState(boardState.playerTwo().position(), puckPosition));
    messagingTemplate.convertAndSend(playerTwoTopic,
        createBroadcastState(GameEngine.mirror(boardState.playerOne().position()), GameEngine.mirror(puckPosition)));
  }
}

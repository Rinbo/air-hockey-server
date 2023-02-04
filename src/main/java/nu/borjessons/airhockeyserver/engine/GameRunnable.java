package nu.borjessons.airhockeyserver.engine;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class GameRunnable implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(GameRunnable.class);

  private final AtomicReference<GameState> atomicReference;
  private final SimpMessagingTemplate messagingTemplate;
  private final String topicBase;

  public GameRunnable(AtomicReference<GameState> atomicReference, SimpMessagingTemplate messagingTemplate, String topicBase) {
    Objects.requireNonNull(atomicReference, "atomicReference must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
    Objects.requireNonNull(topicBase, "gameId must not be null");

    this.atomicReference = atomicReference;
    this.messagingTemplate = messagingTemplate;
    this.topicBase = topicBase;
  }

  private static BroadcastState createBroadcastState(Position opponentPosition, Position puckPosition) {
    return new BroadcastState(opponentPosition, puckPosition);
  }

  private static Position reverse(Position position) {
    return new Position(1 - position.x(), 1 - position.y());
  }

  @Override
  public void run() {
    logger.info("Starting game loop: {}", topicBase);

    String playerOneTopic = topicBase + "/game-state/player-1";
    String playerTwoTopic = topicBase + "/game-state/player-2";

    while (!Thread.currentThread().isInterrupted()) {
      try {
        broadcast(playerOneTopic, playerTwoTopic);
        Thread.sleep(1000 / GameConstants.FRAME_RATE);
      } catch (InterruptedException e) {
        logger.info("Exiting game loop: {}", topicBase);
        Thread.currentThread().interrupt();
      }
    }
  }

  private void broadcast(String playerOneTopic, String playerTwoTopic) {
    GameState gameState = atomicReference.get();
    Position puckPosition = gameState.puck().position();
    messagingTemplate.convertAndSend(playerOneTopic, createBroadcastState(gameState.playerTwo().position(), puckPosition));
    messagingTemplate.convertAndSend(playerTwoTopic, createBroadcastState(reverse(gameState.playerOne().position()), reverse(puckPosition)));
  }
}

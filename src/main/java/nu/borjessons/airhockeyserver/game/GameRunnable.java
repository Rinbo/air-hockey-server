package nu.borjessons.airhockeyserver.game;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Speed;
import nu.borjessons.airhockeyserver.model.GameId;

class GameRunnable implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(GameRunnable.class);

  private final BoardState boardState;
  private final GameId gameId;
  private final SimpMessagingTemplate messagingTemplate;

  public GameRunnable(BoardState boardState, GameId gameId, SimpMessagingTemplate messagingTemplate) {
    Objects.requireNonNull(boardState, "boardState must not be null");
    Objects.requireNonNull(gameId, "gameId must not be null");
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");

    this.boardState = boardState;
    this.messagingTemplate = messagingTemplate;
    this.gameId = gameId;
  }

  /**
   * Distance normalized for width
   */
  private static double calculatePuckHandleDistance(Position puckPosition, Position handlePosition) {
    return Math.sqrt(
        Math.pow(puckPosition.x() - handlePosition.x(), 2) +
            Math.pow((puckPosition.y() - handlePosition.y()) / GameConstants.BOARD_ASPECT_RATIO, 2));
  }

  private static BroadcastState createBroadcastState(Position opponentPosition, Position puckPosition) {
    return new BroadcastState(opponentPosition, puckPosition);
  }

  private static boolean isSpeedZero(Speed speed) {
    return speed.x() == 0 && speed.y() == 0;
  }

  @Override
  public void run() {
    logger.info("Starting game loop: {}", gameId);

    String playerOneTopic = String.format("/topic/game/%s/board-state/player-1", gameId);
    String playerTwoTopic = String.format("/topic/game/%s/board-state/player-2", gameId);

    while (!Thread.currentThread().isInterrupted()) {
      try {
        Collision collision = detectCollision();
        handleCollision(collision);
        tickBoardState();
        broadcast(playerOneTopic, playerTwoTopic);
        TimeUnit.MILLISECONDS.sleep(1000 / GameConstants.FRAME_RATE);
      } catch (InterruptedException e) {
        logger.info("Interrupt called on gameThread: {}", gameId);
        Thread.currentThread().interrupt();
      }
    }

    logger.info("exiting game loop: {}", gameId);
  }

  private void broadcast(String playerOneTopic, String playerTwoTopic) {
    Position puckPosition = boardState.puck().getPosition();
    messagingTemplate.convertAndSend(playerOneTopic,
        createBroadcastState(boardState.playerTwo().getPosition(), puckPosition));
    messagingTemplate.convertAndSend(playerTwoTopic,
        createBroadcastState(GameEngine.mirror(boardState.playerOne().getPosition()), GameEngine.mirror(puckPosition)));
  }

  private Collision detectCollision() {
    Position puckPosition = boardState.puck().getPosition();

    if ((puckPosition.x() - GameConstants.PUCK_RADIUS.x()) <= 0)
      return Collision.LEFT_WALL;

    if ((puckPosition.x() + GameConstants.PUCK_RADIUS.x()) >= 1)
      return Collision.RIGHT_WALL;

    if ((puckPosition.y() + GameConstants.PUCK_RADIUS.y()) >= 1)
      return Collision.BOTTOM_WALL;

    if ((puckPosition.y() - GameConstants.PUCK_RADIUS.y()) <= 0)
      return Collision.TOP_WALL;

    if (calculatePuckHandleDistance(puckPosition, boardState.playerOne().getPosition()) <= GameConstants.PUCK_HANDLE_MIN_DISTANCE)
      return Collision.P1_HANDLE;

    if (calculatePuckHandleDistance(puckPosition, boardState.playerTwo().getPosition()) <= GameConstants.PUCK_HANDLE_MIN_DISTANCE)
      return Collision.P2_HANDLE;

    return Collision.NO_COLLISION;
  }

  private void handleCollision(Collision collision) {
    switch (collision) {
      case LEFT_WALL, RIGHT_WALL -> reversePuckXDirection();
      case TOP_WALL, BOTTOM_WALL -> reversePuckYDirection();
      case P1_HANDLE -> onP1HandleCollision();
      case P2_HANDLE -> onP2HandleCollision();
      case NO_COLLISION -> logger.debug("no collision");
      default -> logger.warn("unknown collision type: {}", collision);
    }
  }

  private void onP1HandleCollision() {
    onPlayerHandleCollision(BoardState::playerOne);
  }

  private void onP2HandleCollision() {
    onPlayerHandleCollision(BoardState::playerTwo);
  }

  private void onPlayerHandleCollision(Function<BoardState, Handle> handleSelector) {
    Speed handleSpeed = handleSelector.apply(boardState).getSpeed();
    Puck puck = boardState.puck();

    if (isSpeedZero(handleSpeed)) {
      puck.ricochet();
    } else {
      puck.setSpeed(handleSpeed);
    }
  }

  private void reversePuckXDirection() {
    updatePuckSpeed(speed -> new Speed(-1 * speed.x(), speed.y()));
  }

  private void reversePuckYDirection() {
    updatePuckSpeed(speed -> new Speed(speed.x(), -1 * speed.y()));
  }

  private void tickBoardState() {
    Puck puck = boardState.puck();
    puck.move();

    Handle playerOne = boardState.playerOne();
    playerOne.updateSpeed();

    Handle playerTwo = boardState.playerTwo();
    playerTwo.updateSpeed();
  }

  private void updatePuckSpeed(UnaryOperator<Speed> speedUpdater) {
    Puck puck = boardState.puck();
    puck.setSpeed(speedUpdater.apply(puck.getSpeed()));
  }
}
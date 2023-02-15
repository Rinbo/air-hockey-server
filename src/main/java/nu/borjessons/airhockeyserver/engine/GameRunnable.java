package nu.borjessons.airhockeyserver.engine;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.UnaryOperator;

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

  // TODO implement max speed
  private static GameObject bounceOffHandle(GameObject puck) {
    return new GameObject(puck.position(), reverseSpeed(puck.speed()));
  }

  private static double calculatePuckHandleDistance(Position puckPosition, Position handlePosition) {
    return Math.sqrt(Math.pow(puckPosition.x() - handlePosition.x(), 2) + Math.pow(puckPosition.y() - handlePosition.y(), 2));
  }

  private static BroadcastState createBroadcastState(Position opponentPosition, Position puckPosition) {
    return new BroadcastState(opponentPosition, puckPosition);
  }

  private static GameObject impartHandleSpeedOnPuck(GameObject puck, GameObject player) {
    return new GameObject(puck.position(), player.speed());
  }

  private static boolean isSpeedZero(Speed speed) {
    return speed.x() == 0 && speed.y() == 0;
  }

  private static Speed reverseSpeed(Speed speed) {
    return new Speed(speed.x() * -1, speed.y() * -1);
  }

  @Override
  public void run() {
    logger.info("Starting game loop: {}", gameId);

    String playerOneTopic = String.format("/topic/game/%s/board-state/player-1", gameId);
    String playerTwoTopic = String.format("/topic/game/%s/board-state/player-2", gameId);

    while (!Thread.currentThread().isInterrupted()) {
      try {
        Collision collision = detectCollision();
        //logger.info("{}", collision);
        handleCollision(collision);
        updatePuckPositionAndSpeed();
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
    BoardState boardState = atomicReference.get();
    Position puckPosition = boardState.puck().position();
    messagingTemplate.convertAndSend(playerOneTopic,
        createBroadcastState(boardState.playerTwo().position(), puckPosition));
    messagingTemplate.convertAndSend(playerTwoTopic,
        createBroadcastState(GameEngine.mirror(boardState.playerOne().position()), GameEngine.mirror(puckPosition)));
  }

  private Collision detectCollision() {
    BoardState boardState = atomicReference.get();
    Position puckPosition = boardState.puck().position();

    if ((puckPosition.x() - GameConstants.PUCK_W_RADIUS) <= 0)
      return Collision.LEFT_WALL;

    if ((puckPosition.x() + GameConstants.PUCK_W_RADIUS) >= 1)
      return Collision.RIGHT_WALL;

    if ((puckPosition.y() + GameConstants.PUCK_H_RADIUS) >= 1)
      return Collision.BOTTOM_WALL;

    if ((puckPosition.y() - GameConstants.PUCK_H_RADIUS) <= 0)
      return Collision.TOP_WALL;

    if (calculatePuckHandleDistance(puckPosition, boardState.playerOne().position()) <= GameConstants.PUCK_HANDLE_MIN_DISTANCE)
      return Collision.P1_HANDLE;

    if (calculatePuckHandleDistance(puckPosition, boardState.playerTwo().position()) <= GameConstants.PUCK_HANDLE_MIN_DISTANCE)
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

  private void onPlayerHandleCollision(Function<BoardState, GameObject> playerSelector) {
    atomicReference.getAndUpdate(boardState -> {
      GameObject player = playerSelector.apply(boardState);
      GameObject puck = boardState.puck();

      GameObject newPuckState = isSpeedZero(player.speed()) ? bounceOffHandle(puck) : impartHandleSpeedOnPuck(puck, player);

      return new BoardState(
          newPuckState,
          boardState.playerOne(),
          boardState.playerTwo());
    });
  }

  private void reversePuckXDirection() {
    updatePuckSpeed(speed -> new Speed(-1 * speed.x(), speed.y()));
  }

  private void reversePuckYDirection() {
    updatePuckSpeed(speed -> new Speed(speed.x(), -1 * speed.y()));
  }

  private void updatePuckPositionAndSpeed() {
    atomicReference.getAndUpdate(boardState -> {
      //logger.info("boardState: {}", boardState);
      GameObject puck = boardState.puck();
      Position position = puck.position();
      Speed speed = puck.speed(); // TODO add board friction

      return new BoardState(
          new GameObject(new Position(position.x() + speed.x(), position.y() + speed.y()), speed),
          boardState.playerOne(),
          boardState.playerTwo());
    });
  }

  private void updatePuckSpeed(UnaryOperator<Speed> speedUpdater) {
    atomicReference.getAndUpdate(boardState -> {
      GameObject puck = boardState.puck();

      return new BoardState(
          new GameObject(puck.position(), speedUpdater.apply(puck.speed())),
          boardState.playerOne(),
          boardState.playerTwo());
    });
  }
}

package nu.borjessons.airhockeyserver.game;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;
import nu.borjessons.airhockeyserver.game.properties.Collision;
import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.game.properties.Speed;
import nu.borjessons.airhockeyserver.game.properties.Vector;
import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.repository.GameStoreController;

class GameRunnable implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(GameRunnable.class);

  private final BoardState boardState;
  private final GameId gameId;
  private final GameStoreController gameStoreController;
  private final ScheduledExecutorService scheduledExecutorService;

  public GameRunnable(BoardState boardState, GameId gameId, GameStoreController gameStoreController, ScheduledExecutorService scheduledExecutorService) {
    Objects.requireNonNull(boardState, "boardState must not be null");
    Objects.requireNonNull(gameId, "gameId must not be null");
    Objects.requireNonNull(gameStoreController, "gameStoreController must not be null");

    this.boardState = boardState;
    this.gameStoreController = gameStoreController;
    this.gameId = gameId;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  /**
   * y-distance normalized for width by dividing by board aspect ratio
   */
  private static double calculatePuckHandleDistance(Position puckPosition, Position handlePosition) {
    double xDiff = puckPosition.x() - handlePosition.x();
    double yDiff = puckPosition.y() - handlePosition.y();
    return Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff / GameConstants.BOARD_ASPECT_RATIO, 2));
  }

  private static BroadcastState createBroadcastState(Position opponentPosition, Position puckPosition, long remainingSeconds) {
    return new BroadcastState(opponentPosition, puckPosition, remainingSeconds);
  }

  private static boolean isBottomWallHit(Position puckPosition) {
    return puckPosition.y() + GameConstants.PUCK_RADIUS.y() >= 1 &&
        (puckPosition.x() < 0.5 - GameConstants.GOAL_WIDTH ||
            puckPosition.x() > 0.5 + GameConstants.GOAL_WIDTH);
  }

  private static boolean isSpeedZero(Speed speed) {
    return speed.x() == 0 && speed.y() == 0;
  }

  private static boolean isTopWallHit(Position puckPosition) {
    return puckPosition.y() - GameConstants.PUCK_RADIUS.y() <= 0 &&
        (puckPosition.x() < 0.5 - GameConstants.GOAL_WIDTH ||
            puckPosition.x() > 0.5 + GameConstants.GOAL_WIDTH);
  }

  private static boolean puckHandleCollision(Position puckPosition, Supplier<Handle> supplier) {
    return calculatePuckHandleDistance(puckPosition, supplier.get().getPosition()) <= GameConstants.PUCK_HANDLE_MIN_DISTANCE;
  }

  @Override
  public void run() {
    logger.info("Starting game loop: {}", gameId);

    ScheduledFuture<String> schedule = scheduledExecutorService.schedule(() -> "complete", GameConstants.GAME_DURATION.getSeconds(), TimeUnit.SECONDS);

    while (!Thread.currentThread().isInterrupted()) {
      try {
        isGameComplete(schedule.isDone());
        Collision collision = detectCollision();
        handleCollision(collision);
        tickBoardState();
        broadcast(schedule.getDelay(TimeUnit.SECONDS));
        TimeUnit.MILLISECONDS.sleep(1000 / GameConstants.FRAME_RATE);
      } catch (InterruptedException e) {
        logger.info("Interrupt called on gameThread: {}", gameId);
        Thread.currentThread().interrupt();
      }
    }

    logger.info("exiting game loop: {}", gameId);
  }

  private void broadcast(long remainingSeconds) {
    Position puckPosition = boardState.puck().getPosition();
    Position playerOneHandlePosition = boardState.playerOne().getPosition();
    Position playerTwoHandlePosition = boardState.playerTwo().getPosition();
    gameStoreController.broadcast(
        createBroadcastState(playerTwoHandlePosition, puckPosition, remainingSeconds),
        createBroadcastState(GameEngine.mirror(playerOneHandlePosition), GameEngine.mirror(puckPosition), remainingSeconds));
  }

  private Collision detectCollision() {
    Position puckPosition = boardState.puck().getPosition();

    if (puckPosition.equals(GameConstants.OFF_BOARD_POSITION)) return Collision.NO_COLLISION;
    if (puckPosition.y() - GameConstants.PUCK_RADIUS.y() > 1) return Collision.P1_GOAL;
    if (puckPosition.y() + GameConstants.PUCK_RADIUS.y() < 0) return Collision.P2_GOAL;
    if (isTopWallHit(puckPosition)) return Collision.TOP_WALL;
    if (isBottomWallHit(puckPosition)) return Collision.BOTTOM_WALL;
    if ((puckPosition.x() - GameConstants.PUCK_RADIUS.x()) <= 0) return Collision.LEFT_WALL;
    if ((puckPosition.x() + GameConstants.PUCK_RADIUS.x()) >= 1) return Collision.RIGHT_WALL;
    if (puckHandleCollision(puckPosition, boardState::playerOne)) return Collision.P1_HANDLE;
    if (puckHandleCollision(puckPosition, boardState::playerTwo)) return Collision.P2_HANDLE;

    return Collision.NO_COLLISION;
  }

  private void handleCollision(Collision collision) {
    switch (collision) {
      case LEFT_WALL -> onLeftWallCollision();
      case RIGHT_WALL -> onRightWallCollision();
      case TOP_WALL -> onTopWallCollision();
      case BOTTOM_WALL -> onBottomWallCollision();
      case P1_HANDLE -> onPuckHandleCollision(BoardState::playerOne);
      case P2_HANDLE -> onPuckHandleCollision(BoardState::playerTwo);
      case P1_GOAL -> onPlayerScores(Agency.PLAYER_2);
      case P2_GOAL -> onPlayerScores(Agency.PLAYER_1);
      case NO_COLLISION -> logger.debug("no collision");
      default -> logger.warn("unknown collision type: {}", collision);
    }
  }

  private void isGameComplete(boolean isComplete) {
    if (isComplete) {
      gameStoreController.gameComplete();
      boardState.resetObjects();
    }
  }

  private void onBottomWallCollision() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(puck.getPosition().x(), 1 - puck.getRadius().y()));
    updatePuckSpeed(speed -> new Speed(speed.x(), -1 * speed.y()));
  }

  // TODO this and the above can be added to puck mechanics
  private void onLeftWallCollision() {
    Puck puck = boardState.puck();
    updatePuckSpeed(speed -> new Speed(-1 * speed.x(), speed.y()));
    puck.setPosition(new Position(0 + puck.getRadius().x(), puck.getPosition().y()));
  }

  private void onPlayerScores(Agency player) {
    gameStoreController.updatePlayerScore(player);
    Puck puck = boardState.puck();
    puck.setPosition(GameConstants.OFF_BOARD_POSITION);
    puck.setSpeed(GameConstants.ZERO_SPEED);
    Position position = player == Agency.PLAYER_1 ? GameConstants.PUCK_START_P2 : GameConstants.PUCK_START_P1;
    scheduledExecutorService.schedule(() -> puck.setPosition(position), GameConstants.PUCK_RESET_DURATION.getSeconds(), TimeUnit.SECONDS);
  }

  private void onPuckHandleCollision(Function<BoardState, Handle> handleSelector) {
    Handle handle = handleSelector.apply(boardState);
    Speed handleSpeed = handle.getSpeed();
    Puck puck = boardState.puck();

    Vector vector = Vector.from(handle.getPosition(), puck.getPosition());
    puck.offsetCollisionWith(handle, vector.angle());

    if (isSpeedZero(handleSpeed)) {
      puck.ricochet(vector);
    } else {
      puck.setSpeed(handleSpeed);
      puck.resetFriction();
    }
  }

  private void onRightWallCollision() {
    Puck puck = boardState.puck();
    updatePuckSpeed(speed -> new Speed(-1 * speed.x(), speed.y()));
    puck.setPosition(new Position(1 - puck.getRadius().x(), puck.getPosition().y()));
  }

  private void onTopWallCollision() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(puck.getPosition().x(), 0 + puck.getRadius().y()));
    updatePuckSpeed(speed -> new Speed(speed.x(), -1 * speed.y()));
  }

  private void tickBoardState() {
    boardState.puck().onTick();
    boardState.playerOne().updateSpeed();
    boardState.playerTwo().updateSpeed();
  }

  private void updatePuckSpeed(UnaryOperator<Speed> speedUpdater) {
    Puck puck = boardState.puck();
    puck.setSpeed(speedUpdater.apply(puck.getSpeed()));
  }
}

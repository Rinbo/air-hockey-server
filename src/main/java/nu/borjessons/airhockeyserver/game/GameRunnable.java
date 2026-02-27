package nu.borjessons.airhockeyserver.game;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

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
import nu.borjessons.airhockeyserver.repository.GameStoreConnector;

class GameRunnable implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(GameRunnable.class);
  private static final long FRAME_DURATION_NS = 1_000_000_000L / GameConstants.FRAME_RATE;
  private static final long GAME_DURATION_NS = GameConstants.GAME_DURATION.toNanos();
  private static final long PUCK_RESET_DURATION_NS = GameConstants.PUCK_RESET_DURATION.toNanos();

  private final BoardState boardState;
  private final BroadcastState p1State = new BroadcastState();
  private final BroadcastState p2State = new BroadcastState();
  private final GameId gameId;
  private final GameStoreConnector gameStoreConnector;

  // Puck reset state: when > 0, the puck is waiting to be placed back on the
  // board
  private long puckResetRemainingNs;
  private Position puckResetTarget;

  public GameRunnable(BoardState boardState, GameId gameId, GameStoreConnector gameStoreConnector) {
    Objects.requireNonNull(boardState, "boardState must not be null");
    Objects.requireNonNull(gameId, "gameId must not be null");
    Objects.requireNonNull(gameStoreConnector, "gameStoreController must not be null");

    this.boardState = boardState;
    this.gameStoreConnector = gameStoreConnector;
    this.gameId = gameId;
  }

  /**
   * y-distance normalized for width by dividing by board aspect ratio
   */
  private static double calculatePuckHandleDistance(Position puckPosition, Position handlePosition) {
    double xDiff = puckPosition.x() - handlePosition.x();
    double yDiff = puckPosition.y() - handlePosition.y();
    return Math
        .sqrt(xDiff * xDiff + (yDiff / GameConstants.BOARD_ASPECT_RATIO) * (yDiff / GameConstants.BOARD_ASPECT_RATIO));
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
    return calculatePuckHandleDistance(puckPosition,
        supplier.get().getPosition()) <= GameConstants.PUCK_HANDLE_MIN_DISTANCE;
  }

  @Override
  public void run() {
    logger.info("Starting game loop: {}", gameId);

    long gameStartNs = System.nanoTime();
    long previousFrameNs = gameStartNs;

    while (!Thread.currentThread().isInterrupted()) {
      try {
        long now = System.nanoTime();
        long elapsedSinceStart = now - gameStartNs;
        long frameDelta = now - previousFrameNs;
        previousFrameNs = now;

        // Check game over
        if (elapsedSinceStart >= GAME_DURATION_NS) {
          gameStoreConnector.gameComplete();
          boardState.resetObjects();
          break;
        }

        // Tick puck reset timer
        if (puckResetRemainingNs > 0) {
          puckResetRemainingNs -= frameDelta;
          if (puckResetRemainingNs <= 0) {
            boardState.puck().setPosition(puckResetTarget);
            puckResetTarget = null;
          }
        }

        Collision collision = detectCollision();
        handleCollision(collision);
        tickBoardState();

        long remainingSeconds = (GAME_DURATION_NS - elapsedSinceStart) / 1_000_000_000L;
        broadcast(remainingSeconds);

        // Sleep precisely for the remaining frame time
        long frameEnd = System.nanoTime();
        long sleepNs = FRAME_DURATION_NS - (frameEnd - now);
        if (sleepNs > 0) {
          Thread.sleep(sleepNs / 1_000_000, (int) (sleepNs % 1_000_000));
        }
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

    p1State.set(playerTwoHandlePosition, puckPosition, remainingSeconds);
    p2State.setMirrored(playerOneHandlePosition, puckPosition, remainingSeconds);
    gameStoreConnector.broadcast(p1State, p2State);
  }

  Collision detectCollision() {
    Position puckPosition = boardState.puck().getPosition();

    if (puckPosition.equals(GameConstants.OFF_BOARD_POSITION))
      return Collision.NO_COLLISION;
    if (puckPosition.y() - GameConstants.PUCK_RADIUS.y() > 1)
      return Collision.P1_GOAL;
    if (puckPosition.y() + GameConstants.PUCK_RADIUS.y() < 0)
      return Collision.P2_GOAL;
    if (isTopWallHit(puckPosition))
      return Collision.TOP_WALL;
    if (isBottomWallHit(puckPosition))
      return Collision.BOTTOM_WALL;
    if ((puckPosition.x() - GameConstants.PUCK_RADIUS.x()) <= 0)
      return Collision.LEFT_WALL;
    if ((puckPosition.x() + GameConstants.PUCK_RADIUS.x()) >= 1)
      return Collision.RIGHT_WALL;
    if (puckHandleCollision(puckPosition, boardState::playerOne))
      return Collision.P1_HANDLE;
    if (puckHandleCollision(puckPosition, boardState::playerTwo))
      return Collision.P2_HANDLE;

    return Collision.NO_COLLISION;
  }

  void handleCollision(Collision collision) {
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

  private void onBottomWallCollision() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(puck.getPosition().x(), 1 - puck.getRadius().y()));
    puck.negateSpeedY();
  }

  private void onLeftWallCollision() {
    Puck puck = boardState.puck();
    puck.negateSpeedX();
    puck.setPosition(new Position(0 + puck.getRadius().x(), puck.getPosition().y()));
  }

  private void onPlayerScores(Agency player) {
    gameStoreConnector.updatePlayerScore(player);
    Puck puck = boardState.puck();
    puck.setPosition(GameConstants.OFF_BOARD_POSITION);
    puck.setSpeedXY(0, 0);

    puckResetTarget = player == Agency.PLAYER_1 ? GameConstants.PUCK_START_P2 : GameConstants.PUCK_START_P1;
    puckResetRemainingNs = PUCK_RESET_DURATION_NS;
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
    puck.negateSpeedX();
    puck.setPosition(new Position(1 - puck.getRadius().x(), puck.getPosition().y()));
  }

  private void onTopWallCollision() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(puck.getPosition().x(), 0 + puck.getRadius().y()));
    puck.negateSpeedY();
  }

  private void tickBoardState() {
    boardState.puck().onTick();
    boardState.playerOne().updateSpeed();
    boardState.playerTwo().updateSpeed();
  }
}

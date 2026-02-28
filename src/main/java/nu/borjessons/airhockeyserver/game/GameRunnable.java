package nu.borjessons.airhockeyserver.game;

import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;
import nu.borjessons.airhockeyserver.game.properties.Collision;
import nu.borjessons.airhockeyserver.game.properties.GameConstants;
import nu.borjessons.airhockeyserver.game.properties.PhysicsSpace;
import nu.borjessons.airhockeyserver.game.properties.Position;
import nu.borjessons.airhockeyserver.model.Agency;
import nu.borjessons.airhockeyserver.model.GameId;
import nu.borjessons.airhockeyserver.repository.GameStoreConnector;

class GameRunnable implements Runnable {
  private static final long FRAME_DURATION_NS = 1_000_000_000L / GameConstants.FRAME_RATE;
  private static final long GAME_DURATION_NS = GameConstants.GAME_DURATION.toNanos();
  private static final long PUCK_RESET_DURATION_NS = GameConstants.PUCK_RESET_DURATION.toNanos();
  private static final Logger logger = LoggerFactory.getLogger(GameRunnable.class);
  private final boolean aiMode;
  private final BoardState boardState;
  private final BroadcastState p1State = new BroadcastState();
  private final BroadcastState p2State = new BroadcastState();
  private final GameId gameId;
  private final GameStoreConnector gameStoreConnector;

  // Puck reset state: when > 0, the puck is waiting to be placed back on the
  // board
  private long puckResetRemainingNs;
  private Position puckResetTarget;

  public GameRunnable(BoardState boardState, GameId gameId, GameStoreConnector gameStoreConnector, boolean aiMode) {
    Objects.requireNonNull(boardState, "boardState must not be null");
    Objects.requireNonNull(gameId, "gameId must not be null");
    Objects.requireNonNull(gameStoreConnector, "gameStoreController must not be null");

    this.aiMode = aiMode;
    this.boardState = boardState;
    this.gameStoreConnector = gameStoreConnector;
    this.gameId = gameId;
  }

  /**
   * Minimum distance between point P and line segment AB in physical space.
   */
  private static double segmentPointDistance(
      double px, double py, double ax, double ay, double bx, double by) {
    double abx = bx - ax, aby = by - ay;
    double apx = px - ax, apy = py - ay;
    double ab2 = abx * abx + aby * aby;

    if (ab2 < 1e-12) {
      return Math.sqrt(apx * apx + apy * apy);
    }

    double t = Math.max(0, Math.min(1, (apx * abx + apy * aby) / ab2));
    double dx = ax + t * abx - px;
    double dy = ay + t * aby - py;
    return Math.sqrt(dx * dx + dy * dy);
  }

  private static boolean isBottomWallHit(Position puckPosition) {
    return puckPosition.y() + GameConstants.PUCK_RADIUS.y() >= 1 &&
        (puckPosition.x() < 0.5 - GameConstants.GOAL_WIDTH ||
            puckPosition.x() > 0.5 + GameConstants.GOAL_WIDTH);
  }

  private static boolean isTopWallHit(Position puckPosition) {
    return puckPosition.y() - GameConstants.PUCK_RADIUS.y() <= 0 &&
        (puckPosition.x() < 0.5 - GameConstants.GOAL_WIDTH ||
            puckPosition.x() > 0.5 + GameConstants.GOAL_WIDTH);
  }

  /**
   * Checks if the puck collides with the handle's swept path (previous → current
   * position). This prevents fast-moving handles from passing through the puck.
   */
  private static boolean puckHandleCollision(Position puckPosition, Handle handle) {
    double px = puckPosition.x();
    double py = PhysicsSpace.toPhysicalY(puckPosition.y());
    Position curr = handle.getPosition();
    Position prev = handle.getPreviousPosition();

    return segmentPointDistance(px, py,
        prev.x(), PhysicsSpace.toPhysicalY(prev.y()),
        curr.x(), PhysicsSpace.toPhysicalY(curr.y())) <= GameConstants.PUCK_HANDLE_MIN_DISTANCE;
  }

  @Override
  public void run() {
    logger.info("Starting game loop: {}", gameId);

    long gameStartNs = System.nanoTime();
    long previousFrameNs = gameStartNs;

    while (!Thread.currentThread().isInterrupted()) {
      long now = System.nanoTime();
      long elapsedSinceStart = now - gameStartNs;
      long frameDelta = now - previousFrameNs;
      previousFrameNs = now;

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

      if (aiMode) {
        AiPlayer.tick(boardState);
      }

      boardState.puck().onTick();
      Collision collision = detectCollision();
      handleCollision(collision);
      updateHandleSpeeds();

      long remainingSeconds = (GAME_DURATION_NS - elapsedSinceStart) / 1_000_000_000L;
      broadcast(remainingSeconds);

      // Park for the remaining frame time
      long sleepNs = FRAME_DURATION_NS - (System.nanoTime() - now);
      if (sleepNs > 0) {
        LockSupport.parkNanos(sleepNs);
      }
    }

    logger.info("exiting game loop: {}", gameId);
  }

  Collision detectCollision() {
    Position puckPosition = boardState.puck().getPosition();

    if (puckPosition.equals(GameConstants.OFF_BOARD_POSITION))
      return Collision.NO_COLLISION;

    boolean inGoalZoneX = puckPosition.x() >= 0.5 - GameConstants.GOAL_WIDTH
        && puckPosition.x() <= 0.5 + GameConstants.GOAL_WIDTH;

    // Puck past bottom edge
    if (puckPosition.y() - GameConstants.PUCK_RADIUS.y() > 1) {
      return inGoalZoneX ? Collision.P1_GOAL : Collision.BOTTOM_WALL;
    }
    // Puck past top edge
    if (puckPosition.y() + GameConstants.PUCK_RADIUS.y() < 0) {
      return inGoalZoneX ? Collision.P2_GOAL : Collision.TOP_WALL;
    }

    if (isTopWallHit(puckPosition))
      return Collision.TOP_WALL;
    if (isBottomWallHit(puckPosition))
      return Collision.BOTTOM_WALL;
    if ((puckPosition.x() - GameConstants.PUCK_RADIUS.x()) <= 0)
      return Collision.LEFT_WALL;
    if ((puckPosition.x() + GameConstants.PUCK_RADIUS.x()) >= 1)
      return Collision.RIGHT_WALL;
    if (puckHandleCollision(puckPosition, boardState.playerOne()))
      return Collision.P1_HANDLE;
    if (puckHandleCollision(puckPosition, boardState.playerTwo()))
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

  private void broadcast(long remainingSeconds) {
    Position puckPosition = boardState.puck().getPosition();
    Position playerOneHandlePosition = boardState.playerOne().getPosition();
    Position playerTwoHandlePosition = boardState.playerTwo().getPosition();

    p1State.set(playerTwoHandlePosition, puckPosition, remainingSeconds);
    p2State.setMirrored(playerOneHandlePosition, puckPosition, remainingSeconds);
    gameStoreConnector.broadcast(p1State, p2State);
  }

  private void onBottomWallCollision() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(puck.getPosition().x(), 1 - puck.getRadius().y()));
    puck.setSpeedXY(puck.getSpeedX(), -Math.abs(puck.getSpeedY()) * GameConstants.WALL_RESTITUTION);
  }

  private void onLeftWallCollision() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(puck.getRadius().x(), puck.getPosition().y()));
    puck.setSpeedXY(Math.abs(puck.getSpeedX()) * GameConstants.WALL_RESTITUTION, puck.getSpeedY());
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
    Puck puck = boardState.puck();

    // --- Physical-space positions ---
    double pxP = puck.getPosition().x();
    double pyP = PhysicsSpace.toPhysicalY(puck.getPosition().y());

    Position curr = handle.getPosition();
    Position prev = handle.getPreviousPosition();
    double hxP = curr.x();
    double hyP = PhysicsSpace.toPhysicalY(curr.y());
    double hpxP = prev.x();
    double hpyP = PhysicsSpace.toPhysicalY(prev.y());

    // Handle velocity this frame (real movement, not stale)
    double hvx = hxP - hpxP;
    double hvy = hyP - hpyP;

    // --- Collision normal via first-contact point ---
    // Find the earliest point along the handle's swept path (A→B) where it
    // enters the puck's collision zone (circle of radius r around the puck).
    // This models the physical moment of first contact, giving a well-defined
    // normal even when the handle passes straight through the puck center.
    double dx = hxP - hpxP, dy = hyP - hpyP; // D = B - A
    double fx = hpxP - pxP, fy = hpyP - pyP; // F = A - P
    double r = GameConstants.PUCK_HANDLE_MIN_DISTANCE;

    double a = dx * dx + dy * dy;
    double b = 2 * (fx * dx + fy * dy);
    double c = fx * fx + fy * fy - r * r;

    double collisionHx, collisionHy;
    if (a < 1e-12) {
      // Handle didn't move — use current position
      collisionHx = hxP;
      collisionHy = hyP;
    } else {
      double disc = b * b - 4 * a * c;
      double t;
      if (disc < 0) {
        // Numerical edge case — fall back to closest point on segment
        t = Math.max(0, Math.min(1, -b / (2 * a)));
      } else {
        // Smaller root = first entry into the collision zone
        t = (-b - Math.sqrt(disc)) / (2 * a);
        t = Math.max(0, Math.min(1, t));
      }
      collisionHx = hpxP + t * dx;
      collisionHy = hpyP + t * dy;
    }

    double nx = pxP - collisionHx;
    double ny = pyP - collisionHy;
    double nDist = Math.sqrt(nx * nx + ny * ny);

    if (nDist < 1e-12) {
      // Still degenerate (handle started exactly on puck center) — use movement
      // direction
      double moveMag = Math.sqrt(dx * dx + dy * dy);
      if (moveMag > 1e-12) {
        nx = dx / moveMag;
        ny = dy / moveMag;
      } else {
        nx = 0;
        ny = -1;
      }
    } else {
      nx /= nDist;
      ny /= nDist;
    }

    // --- Elastic collision impulse ---
    double pvx = puck.getSpeedX();
    double pvy = PhysicsSpace.toPhysicalY(puck.getSpeedY());

    double relVn = (pvx - hvx) * nx + (pvy - hvy) * ny;

    if (relVn < 0) {
      double e = GameConstants.HANDLE_RESTITUTION;
      pvx -= (1 + e) * relVn * nx;
      pvy -= (1 + e) * relVn * ny;
    }

    // --- Separate puck from handle's current position ---
    double sepX = pxP - hxP;
    double sepY = pyP - hyP;
    double sepDist = Math.sqrt(sepX * sepX + sepY * sepY);
    double minDist = GameConstants.PUCK_HANDLE_MIN_DISTANCE;

    if (sepDist < minDist) {
      if (sepDist < 1e-12) {
        sepX = nx;
        sepY = ny;
      } else {
        sepX /= sepDist;
        sepY /= sepDist;
      }
      double overlap = minDist - sepDist;
      pxP += sepX * (overlap + 1e-6);
      pyP += sepY * (overlap + 1e-6);
    }

    // --- Transform back to normalized space ---
    puck.setPosition(new Position(pxP, PhysicsSpace.toNormalizedY(pyP)));
    puck.setSpeedXY(pvx, PhysicsSpace.toNormalizedY(pvy));
  }

  private void onRightWallCollision() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(1 - puck.getRadius().x(), puck.getPosition().y()));
    puck.setSpeedXY(-Math.abs(puck.getSpeedX()) * GameConstants.WALL_RESTITUTION, puck.getSpeedY());
  }

  private void onTopWallCollision() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(puck.getPosition().x(), puck.getRadius().y()));
    puck.setSpeedXY(puck.getSpeedX(), Math.abs(puck.getSpeedY()) * GameConstants.WALL_RESTITUTION);
  }

  private void updateHandleSpeeds() {
    boardState.playerOne().updateSpeed();
    boardState.playerTwo().updateSpeed();
  }
}

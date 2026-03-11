package se.docksidelabs.airhockeyserver.game;

import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.docksidelabs.airhockeyserver.game.objects.Handle;
import se.docksidelabs.airhockeyserver.game.objects.Puck;
import se.docksidelabs.airhockeyserver.game.properties.Collision;
import se.docksidelabs.airhockeyserver.game.properties.GameConstants;
import se.docksidelabs.airhockeyserver.game.properties.PhysicsSpace;
import se.docksidelabs.airhockeyserver.game.properties.Position;
import se.docksidelabs.airhockeyserver.model.Agency;
import se.docksidelabs.airhockeyserver.model.GameId;
import se.docksidelabs.airhockeyserver.repository.GameStoreConnector;

class GameRunnable implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(GameRunnable.class);

  // ── Timing ───────────────────────────────────────────────────────
  private static final long NANOS_PER_SECOND = 1_000_000_000L;
  private static final long FRAME_DURATION_NS = NANOS_PER_SECOND / GameConstants.FRAME_RATE;
  private static final long GAME_DURATION_NS = GameConstants.GAME_DURATION.toNanos();
  private static final long PUCK_RESET_DURATION_NS = GameConstants.PUCK_RESET_DURATION.toNanos();
  private static final long WARMUP_DURATION_NS = NANOS_PER_SECOND;

  // ── Physics ──────────────────────────────────────────────────────
  private static final int SUB_STEPS = 4;
  private static final double GEOMETRIC_EPSILON = 1e-12;
  private static final double SEPARATION_NUDGE = 1e-6;

  // Consecutive handle-collision ticks after which we suppress
  // bounce impulse (resting contact) and collision sound events.
  private static final int RESTING_CONTACT_IMPULSE_THRESHOLD = 3;
  private static final int RESTING_CONTACT_SOUND_THRESHOLD = 2;

  // ── Dependencies ─────────────────────────────────────────────────
  private final boolean aiMode;
  private final BoardState boardState;
  private final BroadcastState playerOneBroadcast = new BroadcastState();
  private final BroadcastState playerTwoBroadcast = new BroadcastState();
  private final GameId gameId;
  private final GameStoreConnector gameStoreConnector;

  // ── Per-round state ──────────────────────────────────────────────
  private long puckResetRemainingNs;
  private Position puckResetTarget;

  // ── Per-tick state ───────────────────────────────────────────────
  private int currentCollisionEvent = BroadcastState.NO_EVENT;
  private int consecutiveHandleCollisionTicks;

  GameRunnable(BoardState boardState, GameId gameId, GameStoreConnector gameStoreConnector, boolean aiMode) {
    this.boardState = Objects.requireNonNull(boardState, "boardState must not be null");
    this.gameId = Objects.requireNonNull(gameId, "gameId must not be null");
    this.gameStoreConnector = Objects.requireNonNull(gameStoreConnector, "gameStoreConnector must not be null");
    this.aiMode = aiMode;
  }

  // ════════════════════════════════════════════════════════════════
  //  Game Loop
  // ════════════════════════════════════════════════════════════════

  @Override
  public void run() {
    logger.info("Starting game loop: {}", gameId);

    runWarmupPhase();
    runMainPhase();

    logger.info("Exiting game loop: {}", gameId);
  }

  /**
   * Broadcasts the initial board state at 60 Hz so clients can connect
   * and see starting positions. Physics and AI are frozen.
   */
  private void runWarmupPhase() {
    long warmupStartNs = System.nanoTime();
    long fullGameSeconds = GAME_DURATION_NS / NANOS_PER_SECOND;

    while (!Thread.currentThread().isInterrupted()) {
      long frameStartNs = System.nanoTime();
      if (frameStartNs - warmupStartNs >= WARMUP_DURATION_NS) {
        break;
      }

      broadcast(fullGameSeconds);
      sleepUntilNextFrame(frameStartNs);
    }
  }

  private void runMainPhase() {
    long gameStartNs = System.nanoTime();
    long previousFrameNs = gameStartNs;

    while (!Thread.currentThread().isInterrupted()) {
      long frameStartNs = System.nanoTime();
      long elapsedSinceStart = frameStartNs - gameStartNs;
      long frameDelta = frameStartNs - previousFrameNs;
      previousFrameNs = frameStartNs;

      if (elapsedSinceStart >= GAME_DURATION_NS) {
        gameStoreConnector.gameComplete();
        boardState.resetObjects();
        break;
      }

      tickPuckReset(frameDelta);

      if (aiMode) {
        AiPlayer.tick(boardState);
      }

      runSubSteppedPhysics();
      updateHandleSpeeds();

      long remainingSeconds = (GAME_DURATION_NS - elapsedSinceStart) / NANOS_PER_SECOND;
      broadcast(remainingSeconds);
      sleepUntilNextFrame(frameStartNs);
    }
  }

  private void sleepUntilNextFrame(long frameStartNs) {
    long sleepNs = FRAME_DURATION_NS - (System.nanoTime() - frameStartNs);
    if (sleepNs > 0) {
      LockSupport.parkNanos(sleepNs);
    }
  }

  // ════════════════════════════════════════════════════════════════
  //  Puck Reset
  // ════════════════════════════════════════════════════════════════

  private void tickPuckReset(long frameDeltaNs) {
    if (puckResetRemainingNs <= 0) {
      return;
    }

    puckResetRemainingNs -= frameDeltaNs;

    if (puckResetRemainingNs <= 0) {
      boardState.puck().setPosition(puckResetTarget);
      puckResetTarget = null;
    }
  }

  // ════════════════════════════════════════════════════════════════
  //  Sub-stepped Physics
  // ════════════════════════════════════════════════════════════════

  /**
   * Runs collision detection and resolution before moving the puck,
   * preventing overlaps from deepening. Tracks resting contact to
   * suppress bounce impulse and collision sounds.
   */
  private void runSubSteppedPhysics() {
    currentCollisionEvent = BroadcastState.NO_EVENT;
    boolean anyHandleCollision = false;

    for (int step = 0; step < SUB_STEPS; step++) {
      EnumSet<Collision> collisions = detectCollisions();
      handleCollisions(collisions);
      currentCollisionEvent |= toEventMask(collisions);

      if (collisions.contains(Collision.P1_HANDLE) || collisions.contains(Collision.P2_HANDLE)) {
        anyHandleCollision = true;
      }

      boardState.puck().onSubTick(SUB_STEPS);
    }

    consecutiveHandleCollisionTicks = anyHandleCollision
        ? consecutiveHandleCollisionTicks + 1
        : 0;

    if (consecutiveHandleCollisionTicks > RESTING_CONTACT_SOUND_THRESHOLD) {
      currentCollisionEvent = BroadcastState.NO_EVENT;
    }
  }

  // ════════════════════════════════════════════════════════════════
  //  Collision Detection
  // ════════════════════════════════════════════════════════════════

  /** Package-private for testing. */
  EnumSet<Collision> detectCollisions() {
    Position puckPosition = boardState.puck().getPosition();

    if (puckPosition.equals(GameConstants.OFF_BOARD_POSITION)) {
      return EnumSet.noneOf(Collision.class);
    }

    boolean inGoalZoneX = isInGoalZone(puckPosition.x());

    // Goals take absolute priority when puck is fully past an edge
    if (puckPosition.y() - GameConstants.PUCK_RADIUS.y() > 1) {
      return EnumSet.of(inGoalZoneX ? Collision.P1_GOAL : Collision.BOTTOM_WALL);
    }
    if (puckPosition.y() + GameConstants.PUCK_RADIUS.y() < 0) {
      return EnumSet.of(inGoalZoneX ? Collision.P2_GOAL : Collision.TOP_WALL);
    }

    EnumSet<Collision> result = EnumSet.noneOf(Collision.class);

    if (isTouchingTopWall(puckPosition))    result.add(Collision.TOP_WALL);
    if (isTouchingBottomWall(puckPosition)) result.add(Collision.BOTTOM_WALL);
    if (isTouchingLeftWall(puckPosition))   result.add(Collision.LEFT_WALL);
    if (isTouchingRightWall(puckPosition))  result.add(Collision.RIGHT_WALL);
    if (isTouchingHandle(puckPosition, boardState.playerOne())) result.add(Collision.P1_HANDLE);
    if (isTouchingHandle(puckPosition, boardState.playerTwo())) result.add(Collision.P2_HANDLE);

    return result;
  }

  private static boolean isInGoalZone(double x) {
    return x >= 0.5 - GameConstants.GOAL_WIDTH
        && x <= 0.5 + GameConstants.GOAL_WIDTH;
  }

  private static boolean isTouchingTopWall(Position puck) {
    return puck.y() - GameConstants.PUCK_RADIUS.y() <= 0
        && !isInGoalZone(puck.x());
  }

  private static boolean isTouchingBottomWall(Position puck) {
    return puck.y() + GameConstants.PUCK_RADIUS.y() >= 1
        && !isInGoalZone(puck.x());
  }

  private static boolean isTouchingLeftWall(Position puck) {
    return puck.x() - GameConstants.PUCK_RADIUS.x() <= 0;
  }

  private static boolean isTouchingRightWall(Position puck) {
    return puck.x() + GameConstants.PUCK_RADIUS.x() >= 1;
  }

  /**
   * Uses the handle's swept path (previous → current position) for
   * collision detection, preventing fast-moving handles from tunneling.
   */
  private static boolean isTouchingHandle(Position puckPosition, Handle handle) {
    double puckX = puckPosition.x();
    double puckY = PhysicsSpace.toPhysicalY(puckPosition.y());

    Position current = handle.getPosition();
    Position previous = handle.getPreviousPosition();

    double distanceToSweptPath = segmentPointDistance(
        puckX, puckY,
        previous.x(), PhysicsSpace.toPhysicalY(previous.y()),
        current.x(), PhysicsSpace.toPhysicalY(current.y()));

    return distanceToSweptPath <= GameConstants.PUCK_HANDLE_MIN_DISTANCE;
  }

  // ════════════════════════════════════════════════════════════════
  //  Collision Handling
  // ════════════════════════════════════════════════════════════════

  /** Package-private for testing. */
  void handleCollisions(EnumSet<Collision> collisions) {
    for (Collision collision : collisions) {
      switch (collision) {
        case LEFT_WALL   -> bounceOffLeftWall();
        case RIGHT_WALL  -> bounceOffRightWall();
        case TOP_WALL    -> bounceOffTopWall();
        case BOTTOM_WALL -> bounceOffBottomWall();
        case P1_HANDLE   -> resolvePuckHandleCollision(boardState.playerOne());
        case P2_HANDLE   -> resolvePuckHandleCollision(boardState.playerTwo());
        case P1_GOAL     -> awardGoal(Agency.PLAYER_2);
        case P2_GOAL     -> awardGoal(Agency.PLAYER_1);
        case NO_COLLISION -> { }
      }
    }
  }

  // ── Wall Bounces ─────────────────────────────────────────────────

  private void bounceOffLeftWall() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(puck.getRadius().x(), puck.getPosition().y()));
    puck.setSpeedXY(Math.abs(puck.getSpeedX()) * GameConstants.WALL_RESTITUTION, puck.getSpeedY());
  }

  private void bounceOffRightWall() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(1 - puck.getRadius().x(), puck.getPosition().y()));
    puck.setSpeedXY(-Math.abs(puck.getSpeedX()) * GameConstants.WALL_RESTITUTION, puck.getSpeedY());
  }

  private void bounceOffTopWall() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(puck.getPosition().x(), puck.getRadius().y()));
    puck.setSpeedXY(puck.getSpeedX(), Math.abs(puck.getSpeedY()) * GameConstants.WALL_RESTITUTION);
  }

  private void bounceOffBottomWall() {
    Puck puck = boardState.puck();
    puck.setPosition(new Position(puck.getPosition().x(), 1 - puck.getRadius().y()));
    puck.setSpeedXY(puck.getSpeedX(), -Math.abs(puck.getSpeedY()) * GameConstants.WALL_RESTITUTION);
  }

  // ── Goal Scoring ─────────────────────────────────────────────────

  private void awardGoal(Agency scoringPlayer) {
    gameStoreConnector.updatePlayerScore(scoringPlayer);

    Puck puck = boardState.puck();
    puck.setPosition(GameConstants.OFF_BOARD_POSITION);
    puck.setSpeedXY(0, 0);

    puckResetTarget = (scoringPlayer == Agency.PLAYER_1)
        ? GameConstants.PUCK_START_P2
        : GameConstants.PUCK_START_P1;
    puckResetRemainingNs = PUCK_RESET_DURATION_NS;
  }

  // ── Handle Collision (main physics routine) ──────────────────────

  private void resolvePuckHandleCollision(Handle handle) {
    Puck puck = boardState.puck();

    double puckX = puck.getPosition().x();
    double puckY = PhysicsSpace.toPhysicalY(puck.getPosition().y());

    Position handleCurrent = handle.getPosition();
    Position handlePrevious = handle.getPreviousPosition();

    double handleCurrentX  = handleCurrent.x();
    double handleCurrentY  = PhysicsSpace.toPhysicalY(handleCurrent.y());
    double handlePreviousX = handlePrevious.x();
    double handlePreviousY = PhysicsSpace.toPhysicalY(handlePrevious.y());

    double handleVelocityX = handleCurrentX - handlePreviousX;
    double handleVelocityY = handleCurrentY - handlePreviousY;

    double[] collisionNormal = computeCollisionNormal(
        puckX, puckY, handleCurrentX, handleCurrentY, handlePreviousX, handlePreviousY);
    double normalX = collisionNormal[0];
    double normalY = collisionNormal[1];

    double puckVelocityX = puck.getSpeedX();
    double puckVelocityY = PhysicsSpace.toPhysicalY(puck.getSpeedY());

    double[] impulseResult = applyImpulse(
        puckVelocityX, puckVelocityY, handleVelocityX, handleVelocityY, normalX, normalY);

    double[] separated = separateFromHandle(puckX, puckY, handleCurrentX, handleCurrentY, normalX, normalY);

    double clampedX = clampToBoardX(separated[0]);
    double clampedY = clampToBoardY(separated[1], clampedX);

    puck.setPosition(new Position(clampedX, PhysicsSpace.toNormalizedY(clampedY)));
    puck.setSpeedXY(impulseResult[0], PhysicsSpace.toNormalizedY(impulseResult[1]));
  }

  // ── Collision Normal ─────────────────────────────────────────────

  /**
   * Computes the collision normal via ray-circle intersection on the
   * handle's swept path. Returns {normalX, normalY} as a unit vector
   * pointing from the contact point toward the puck.
   */
  private double[] computeCollisionNormal(
      double puckX, double puckY,
      double handleCurrentX, double handleCurrentY,
      double handlePreviousX, double handlePreviousY) {

    double sweepDx = handleCurrentX - handlePreviousX;
    double sweepDy = handleCurrentY - handlePreviousY;
    double sweepLengthSquared = sweepDx * sweepDx + sweepDy * sweepDy;

    double contactX = handleCurrentX;
    double contactY = handleCurrentY;

    if (sweepLengthSquared >= GEOMETRIC_EPSILON) {
      double contactT = findFirstContactParameter(
          puckX, puckY, handlePreviousX, handlePreviousY, sweepDx, sweepDy, sweepLengthSquared);
      contactX = handlePreviousX + contactT * sweepDx;
      contactY = handlePreviousY + contactT * sweepDy;
    }

    double normalX = puckX - contactX;
    double normalY = puckY - contactY;
    double normalLength = Math.sqrt(normalX * normalX + normalY * normalY);

    if (normalLength < GEOMETRIC_EPSILON) {
      return fallbackNormal(sweepDx, sweepDy);
    }

    return new double[] { normalX / normalLength, normalY / normalLength };
  }

  /**
   * Finds the parameter t ∈ [0,1] along the handle's swept segment
   * where it first enters the puck's collision circle.
   */
  private static double findFirstContactParameter(
      double puckX, double puckY,
      double segmentStartX, double segmentStartY,
      double segmentDx, double segmentDy,
      double segmentLengthSquared) {

    double startToPuckX = segmentStartX - puckX;
    double startToPuckY = segmentStartY - puckY;
    double collisionRadius = GameConstants.PUCK_HANDLE_MIN_DISTANCE;

    double quadraticA = segmentLengthSquared;
    double quadraticB = 2 * (startToPuckX * segmentDx + startToPuckY * segmentDy);
    double quadraticC = startToPuckX * startToPuckX + startToPuckY * startToPuckY - collisionRadius * collisionRadius;

    double discriminant = quadraticB * quadraticB - 4 * quadraticA * quadraticC;

    if (discriminant < 0) {
      return Math.clamp(-quadraticB / (2 * quadraticA), 0.0, 1.0);
    }

    // Smaller root = first entry into the collision zone
    double firstEntry = (-quadraticB - Math.sqrt(discriminant)) / (2 * quadraticA);
    return Math.clamp(firstEntry, 0.0, 1.0);
  }

  private static double[] fallbackNormal(double sweepDx, double sweepDy) {
    double sweepLength = Math.sqrt(sweepDx * sweepDx + sweepDy * sweepDy);
    if (sweepLength > GEOMETRIC_EPSILON) {
      return new double[] { sweepDx / sweepLength, sweepDy / sweepLength };
    }
    // Fully degenerate — push puck upward
    return new double[] { 0, -1 };
  }

  // ── Impulse ──────────────────────────────────────────────────────

  /**
   * Applies elastic collision impulse in physical space. If the puck
   * is in prolonged resting contact (trapped against a wall), the speed
   * is zeroed instead of bounced. Returns {velocityX, velocityY}.
   */
  private double[] applyImpulse(
      double puckVelocityX, double puckVelocityY,
      double handleVelocityX, double handleVelocityY,
      double normalX, double normalY) {

    double relativeNormalVelocity = (puckVelocityX - handleVelocityX) * normalX
        + (puckVelocityY - handleVelocityY) * normalY;

    if (relativeNormalVelocity >= 0) {
      return new double[] { puckVelocityX, puckVelocityY };
    }

    if (consecutiveHandleCollisionTicks > RESTING_CONTACT_IMPULSE_THRESHOLD) {
      return new double[] { 0, 0 };
    }

    double impulseFactor = (1 + GameConstants.HANDLE_RESTITUTION) * relativeNormalVelocity;
    return new double[] {
        puckVelocityX - impulseFactor * normalX,
        puckVelocityY - impulseFactor * normalY
    };
  }

  // ── Separation ───────────────────────────────────────────────────

  /**
   * Pushes the puck out of the handle overlap. Full separation is
   * required because the handle has infinite mass (player-controlled).
   * Returns {separatedX, separatedY}.
   */
  private static double[] separateFromHandle(
      double puckX, double puckY,
      double handleX, double handleY,
      double normalX, double normalY) {

    double separationX = puckX - handleX;
    double separationY = puckY - handleY;
    double separationDistance = Math.sqrt(separationX * separationX + separationY * separationY);
    double minimumDistance = GameConstants.PUCK_HANDLE_MIN_DISTANCE;

    if (separationDistance >= minimumDistance) {
      return new double[] { puckX, puckY };
    }

    double directionX;
    double directionY;
    if (separationDistance < GEOMETRIC_EPSILON) {
      directionX = normalX;
      directionY = normalY;
    } else {
      directionX = separationX / separationDistance;
      directionY = separationY / separationDistance;
    }

    double overlap = minimumDistance - separationDistance + SEPARATION_NUDGE;
    return new double[] {
        puckX + directionX * overlap,
        puckY + directionY * overlap
    };
  }

  // ── Board Clamping ───────────────────────────────────────────────

  private static double clampToBoardX(double physicalX) {
    double puckRadiusX = GameConstants.PUCK_RADIUS.x();
    return Math.max(puckRadiusX, Math.min(1.0 - puckRadiusX, physicalX));
  }

  /**
   * Clamps Y to board bounds, but allows the puck to pass through
   * goal openings (no Y clamp when centered in the goal zone).
   */
  private static double clampToBoardY(double physicalY, double clampedX) {
    if (isInGoalZone(clampedX)) {
      return physicalY;
    }

    double puckRadiusYPhysical = PhysicsSpace.toPhysicalY(GameConstants.PUCK_RADIUS.y());
    double maxPhysicalY = PhysicsSpace.toPhysicalY(1.0) - puckRadiusYPhysical;
    return Math.max(puckRadiusYPhysical, Math.min(maxPhysicalY, physicalY));
  }

  // ════════════════════════════════════════════════════════════════
  //  Event Mapping & Broadcasting
  // ════════════════════════════════════════════════════════════════

  private static int toEventMask(EnumSet<Collision> collisions) {
    int event = BroadcastState.NO_EVENT;
    for (Collision collision : collisions) {
      event |= switch (collision) {
        case LEFT_WALL, RIGHT_WALL, TOP_WALL, BOTTOM_WALL -> BroadcastState.WALL_HIT;
        case P1_HANDLE, P2_HANDLE -> BroadcastState.HANDLE_HIT;
        case P1_GOAL, P2_GOAL -> BroadcastState.GOAL;
        case NO_COLLISION -> BroadcastState.NO_EVENT;
      };
    }
    return event;
  }

  private void broadcast(long remainingSeconds) {
    Position puckPosition = boardState.puck().getPosition();
    Position playerOneHandlePosition = boardState.playerOne().getPosition();
    Position playerTwoHandlePosition = boardState.playerTwo().getPosition();

    playerOneBroadcast.set(playerTwoHandlePosition, puckPosition, remainingSeconds, currentCollisionEvent);
    playerTwoBroadcast.setMirrored(playerOneHandlePosition, puckPosition, remainingSeconds, currentCollisionEvent);
    gameStoreConnector.broadcast(playerOneBroadcast, playerTwoBroadcast);
  }

  private void updateHandleSpeeds() {
    boardState.playerOne().updateSpeed();
    boardState.playerTwo().updateSpeed();
  }

  // ════════════════════════════════════════════════════════════════
  //  Geometry Utilities
  // ════════════════════════════════════════════════════════════════

  /**
   * Minimum distance between a point and a line segment in 2D space.
   */
  private static double segmentPointDistance(
      double pointX, double pointY,
      double segmentStartX, double segmentStartY,
      double segmentEndX, double segmentEndY) {

    double segmentDx = segmentEndX - segmentStartX;
    double segmentDy = segmentEndY - segmentStartY;
    double toPointX = pointX - segmentStartX;
    double toPointY = pointY - segmentStartY;
    double segmentLengthSquared = segmentDx * segmentDx + segmentDy * segmentDy;

    if (segmentLengthSquared < GEOMETRIC_EPSILON) {
      return Math.sqrt(toPointX * toPointX + toPointY * toPointY);
    }

    double projection = Math.clamp(
        (toPointX * segmentDx + toPointY * segmentDy) / segmentLengthSquared,
        0.0, 1.0);

    double closestX = segmentStartX + projection * segmentDx - pointX;
    double closestY = segmentStartY + projection * segmentDy - pointY;
    return Math.sqrt(closestX * closestX + closestY * closestY);
  }
}

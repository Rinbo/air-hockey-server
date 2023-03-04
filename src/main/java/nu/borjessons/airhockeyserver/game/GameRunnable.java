package nu.borjessons.airhockeyserver.game;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;
import nu.borjessons.airhockeyserver.game.properties.*;
import nu.borjessons.airhockeyserver.model.GameId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.UnaryOperator;

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
     * y-distance normalized for width by dividing by board aspect ratio
     */
    private static double calculatePuckHandleDistance(Position puckPosition, Position handlePosition) {
        double xDiff = puckPosition.x() - handlePosition.x();
        double yDiff = puckPosition.y() - handlePosition.y();
        return Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff / GameConstants.BOARD_ASPECT_RATIO, 2));
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
        Position playerOneHandlePosition = boardState.playerOne().getPosition();
        Position playerTwoHandlePosition = boardState.playerTwo().getPosition();
        messagingTemplate.convertAndSend(playerOneTopic, createBroadcastState(playerTwoHandlePosition, puckPosition));
        messagingTemplate.convertAndSend(playerTwoTopic, createBroadcastState(GameEngine.mirror(playerOneHandlePosition), GameEngine.mirror(puckPosition)));
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
            case LEFT_WALL, RIGHT_WALL -> updatePuckSpeed(speed -> new Speed(-1 * speed.x(), speed.y()));
            case TOP_WALL, BOTTOM_WALL -> updatePuckSpeed(speed -> new Speed(speed.x(), -1 * speed.y()));
            case P1_HANDLE -> onPlayerHandleCollision(BoardState::playerOne);
            case P2_HANDLE -> onPlayerHandleCollision(BoardState::playerTwo);
            case NO_COLLISION -> logger.debug("no collision");
            default -> logger.warn("unknown collision type: {}", collision);
        }
    }

    private void onPlayerHandleCollision(Function<BoardState, Handle> handleSelector) {
        Handle handle = handleSelector.apply(boardState);
        Speed handleSpeed = handle.getSpeed();
        Puck puck = boardState.puck();

        // The calculations made here could be of use in the ricochet calculation?
        puck.offsetCollisionWith(handle, Vector.from(handle.getPosition(), puck.getPosition()).angle());

        if (isSpeedZero(handleSpeed)) {
            puck.ricochet(handle);
        } else {
            // TODO maybe also include some of the pucks current speed?
            puck.setSpeed(handleSpeed);
        }
    }

    private void tickBoardState() {
        boardState.puck().move();
        boardState.playerOne().updateSpeed();
        boardState.playerTwo().updateSpeed();
    }

    private void updatePuckSpeed(UnaryOperator<Speed> speedUpdater) {
        Puck puck = boardState.puck();
        puck.setSpeed(speedUpdater.apply(puck.getSpeed()));
    }
}

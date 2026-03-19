package se.docksidelabs.airhockeyserver.transport;

import se.docksidelabs.airhockeyserver.game.BroadcastState;
import se.docksidelabs.airhockeyserver.model.Agency;
import se.docksidelabs.airhockeyserver.model.GameId;

/**
 * Abstraction for sending board state to players, decoupling
 * the game loop from the underlying transport mechanism
 * (WebSocket, UDP datagrams via WebTransport, etc.).
 */
public interface BoardTransport {

  /**
   * Send the current board state to a specific player in a game.
   *
   * @param gameId the game identifier
   * @param agency which player to send to
   * @param state  the board state snapshot
   */
  void sendBoardState(GameId gameId, Agency agency, BroadcastState state);
}

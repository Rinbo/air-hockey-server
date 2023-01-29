package nu.borjessons.airhockeyserver.task;

import java.util.TimerTask;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.model.GameId;

public class CountdownTask extends TimerTask {
  private int count = 3;
  private final GameId gameId;
  private final SimpMessagingTemplate messagingTemplate;

  public CountdownTask(GameId gameId, SimpMessagingTemplate messagingTemplate) {
    this.gameId = gameId;
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void run() {

  }
}

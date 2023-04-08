package nu.borjessons.airhockeyserver.worker;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import nu.borjessons.airhockeyserver.repository.UserStore;
import nu.borjessons.airhockeyserver.utils.TopicUtils;

public class PingWorker {
  private final SimpMessagingTemplate messagingTemplate;
  private final UserStore userStore;

  public PingWorker(SimpMessagingTemplate messagingTemplate, UserStore userStore) {
    this.messagingTemplate = messagingTemplate;
    this.userStore = userStore;
  }

  @Scheduled(fixedRate = 10000)
  public void pingUsers() {
    userStore.getAll().forEach(username -> messagingTemplate.convertAndSend(TopicUtils.createPingTopic(username), "ping"));
  }
}

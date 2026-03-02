package se.docksidelabs.airhockeyserver.worker;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import se.docksidelabs.airhockeyserver.repository.UserStore;
import se.docksidelabs.airhockeyserver.utils.TopicUtils;

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

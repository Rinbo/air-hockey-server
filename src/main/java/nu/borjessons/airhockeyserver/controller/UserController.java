package nu.borjessons.airhockeyserver.controller;

import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.UserStore;

@Controller
@RequestMapping("/users")
public class UserController {
  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final UserStore userStore;

  public UserController(SimpMessagingTemplate messagingTemplate, UserStore userStore) {
    Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
    Objects.requireNonNull(userStore, "userStore must not be null");

    this.messagingTemplate = messagingTemplate;
    this.userStore = userStore;
  }

  @MessageMapping("/users/heartbeat")
  public void eHeartbeat(@Payload String username) {
    userStore.heartbeat(new Username(username));
  }

  // TODO add user to session using SimpleHeaderAccessor here instead of in game controller
  @MessageMapping("/users/enter")
  public void enter(@Payload String username, SimpMessageHeaderAccessor header) {
    if (userStore.addUser(new Username(username))) {
      messagingTemplate.convertAndSend("/topic/users", userStore.getAll());
      logger.info("user entered: {}", username);
      return;
    }
    logger.error("Failed to add user to userStore: {}", username);
  }

  @MessageMapping("/users/exit")
  public void exit(@Payload String username, SimpMessageHeaderAccessor header) {
    userStore.removeUser(new Username(username));
    messagingTemplate.convertAndSend("/topic/users", userStore.getAll());
    logger.info("user exited: {}", username);
  }

  @ResponseBody
  @GetMapping
  public Set<Username> getUsers() {
    return userStore.getAll();
  }

  @GetMapping("/{name}/validate")
  public ResponseEntity<Username> validateName(@PathVariable String name) {
    Username username = validateUsername(name, 0);
    return ResponseEntity.ok(username);
  }

  private Username validateUsername(String name, int suffix) {
    Username username = suffix == 0 ? new Username(name) : new Username(name + "-" + suffix);
    if (!userStore.contains(username)) return username;
    return validateUsername(name, suffix + 1);
  }
}

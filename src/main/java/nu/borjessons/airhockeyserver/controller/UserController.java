package nu.borjessons.airhockeyserver.controller;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
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

  @MessageMapping("/users/enter")
  public void enter(@Payload String username) {
    if (userStore.addUser(new Username(username))) {
      messagingTemplate.convertAndSend("/topic/users", userStore.getAll());
      logger.info("user entered: {}", username);
      return;
    }
    logger.error("Failed to add user to userStore: {}", username);
  }

  @MessageMapping("/users/exit")
  public void exit(@Payload String username) {
    userStore.removeUser(new Username(username));
    messagingTemplate.convertAndSend("/topic/users", userStore.getAll());
    logger.info("user exited: {}", username);
  }

  @ResponseBody
  @GetMapping
  public Set<Username> getUsers() {
    return userStore.getAll();
  }

  @GetMapping(value = "/{name}/validate", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Response> validateName(@PathVariable String name) {
    List<String> names = userStore.getAll().stream().map(Username::getTrimmed).map(String::toLowerCase).toList();
    if (names.contains(new Username(name).getTrimmed().toLowerCase())) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(new Response("Already taken"));
    }

    return ResponseEntity.ok(new Response(name));
  }

  private Username validateUsername(String name, int suffix) {
    Username username = suffix == 0 ? new Username(name) : new Username(name + "-" + suffix);
    if (!userStore.contains(username)) return username;
    return validateUsername(name, suffix + 1);
  }

  public record Response(String data) {
  }
}

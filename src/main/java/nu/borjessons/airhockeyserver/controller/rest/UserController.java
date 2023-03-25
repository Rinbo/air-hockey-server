package nu.borjessons.airhockeyserver.controller.rest;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.UserStore;

@RestController
@RequestMapping("/users")
public class UserController {
  private final UserStore userStore;

  public UserController(UserStore userStore) {
    Objects.requireNonNull(userStore, "userStore must not be null");

    this.userStore = userStore;
  }

  @GetMapping("/{name}/validate")
  public ResponseEntity<String> validateName(@PathVariable String name) {
    return ResponseEntity.ok(validateUsername(name, 0).toString());
  }

  private Username validateUsername(String name, int suffix) {
    Username username = suffix == 0 ? new Username(name) : new Username(name + "-" + suffix);

    if (!userStore.contains(username)) return username;

    return validateUsername(name, suffix + 1);
  }
}

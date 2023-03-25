package nu.borjessons.airhockeyserver.controller.rest;

import java.util.HashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.UserStore;

class UserControllerTest {

  @Test
  void validateName() {
    HashSet<Username> users = new HashSet<>();
    Username user1 = new Username("User");
    Username user2 = new Username("User-1");
    users.add(user1);
    users.add(user2);

    UserStore userStore = new UserStore(users);
    UserController userController = new UserController(userStore);

    Assertions.assertEquals("User-2", userController.validateName("User").getBody());
    Assertions.assertEquals("user-2", userController.validateName("user").getBody());
    Assertions.assertEquals("Robin", userController.validateName("Robin").getBody());
  }
}
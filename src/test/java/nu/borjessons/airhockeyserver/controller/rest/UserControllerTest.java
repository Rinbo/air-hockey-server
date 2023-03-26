package nu.borjessons.airhockeyserver.controller.rest;

import java.util.HashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.controller.UserController;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.UserStore;

class UserControllerTest {

  @Test
  void validateName() {
    SimpMessagingTemplate simpMessagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
    HashSet<Username> users = new HashSet<>();
    users.add(new Username("User"));
    users.add(new Username("User-1"));

    UserStore userStore = new UserStore(users);
    UserController userController = new UserController(simpMessagingTemplate, userStore);

    Assertions.assertEquals("User-2", userController.validateName("User").getBody().toString());
    Assertions.assertEquals("user-2", userController.validateName("user").getBody().toString());
    Assertions.assertEquals("Robin", userController.validateName("Robin").getBody().toString());

    Mockito.verifyNoInteractions(simpMessagingTemplate);
  }
}
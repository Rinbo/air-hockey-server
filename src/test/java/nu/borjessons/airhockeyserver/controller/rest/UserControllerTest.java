package nu.borjessons.airhockeyserver.controller.rest;

import java.util.HashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import nu.borjessons.airhockeyserver.controller.UserController;
import nu.borjessons.airhockeyserver.model.Username;
import nu.borjessons.airhockeyserver.repository.UserStore;

class UserControllerTest {

  private static void verifyResponse(ResponseEntity<UserController.Response> response, HttpStatus expectedStatus, UserController.Response expectedBody) {
    Assertions.assertEquals(expectedStatus, response.getStatusCode());
    Assertions.assertEquals(expectedBody, response.getBody());
  }

  @Test
  void validateName() {
    SimpMessagingTemplate simpMessagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
    HashSet<Username> users = new HashSet<>();
    users.add(new Username("User$123"));
    users.add(new Username("User-1"));

    UserStore userStore = new UserStore(users);
    UserController userController = new UserController(simpMessagingTemplate, userStore);

    verifyResponse(userController.validateName("User$234"), HttpStatus.CONFLICT, new UserController.Response("Already taken"));
    verifyResponse(userController.validateName("user"), HttpStatus.CONFLICT, new UserController.Response("Already taken"));
    verifyResponse(userController.validateName("Robin"), HttpStatus.OK, new UserController.Response("Robin"));

    Mockito.verifyNoInteractions(simpMessagingTemplate);
  }
}
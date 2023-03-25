package nu.borjessons.airhockeyserver.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UsernameTest {
  @Test
  void EqualsTest() {
    Username username = new Username("USER");

    Assertions.assertEquals(username, new Username("USER"));
    Assertions.assertEquals(username, new Username("User"));
    Assertions.assertEquals(username, new Username("user"));
    Assertions.assertNotEquals(username, new Username("USER-1"));
  }

  @Test
  void hashcodeTest() {
    Username username = new Username("User");
    Assertions.assertEquals(username.hashCode(), new Username("user").hashCode());
  }
}
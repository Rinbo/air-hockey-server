package nu.borjessons.airhockeyserver.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PlayerTest {
  @Test
  void equalsTest() {
    Player p1 = new Player(Agency.PLAYER_1, new Username("Robin"));
    Player p2 = new Player(Agency.PLAYER_2, new Username("Robin"));

    Assertions.assertEquals(p2, p2);

    Set<Player> players = new HashSet<>();
    players.add(p1);
    players.add(p2);

    Assertions.assertEquals(1, players.size());
  }

  @Test
  void setSortingTest() {
    Player p1 = new Player(Agency.PLAYER_1, new Username("Robin"));
    Player p2 = new Player(Agency.PLAYER_2, new Username("Snorre"));

    Set<Player> players = new HashSet<>();
    players.add(p2);
    players.add(p1);

    List<Player> list = players.stream().sorted(Comparator.comparing(Player::getAgency)).toList();
    Assertions.assertEquals(p1, list.get(0));
    Assertions.assertEquals(p2, list.get(1));
  }
}

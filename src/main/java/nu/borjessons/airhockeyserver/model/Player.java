package nu.borjessons.airhockeyserver.model;

import java.util.Objects;
import java.util.StringJoiner;

import nu.borjessons.airhockeyserver.repository.GameStore;

public class Player {
  private final Agency agency;
  private boolean ready;
  private short score;
  private final Username username;

  public Player(Agency agency, Username username) {
    Objects.requireNonNull(username, "username must not be null");
    Objects.requireNonNull(agency, "agency must not be null");

    this.agency = agency;
    this.username = username;
    this.score = 0;
    this.ready = false;
  }

  /**
   * Equality only determined by name so that no duplicates of players can exist in the
   * {@link GameStore} player set.
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;

    Player other = (Player) object;

    return username.equals(other.username);
  }

  public Agency getAgency() {
    return agency;
  }

  public short getScore() {
    return score;
  }

  public Username getUsername() {
    return username;
  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }

  public boolean isPlayer(Username username) {
    return this.username.equals(username);
  }

  public boolean isReady() {
    return ready;
  }

  public void score() {
    score++;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Player.class.getSimpleName() + "[", "]")
        .add("agency=" + agency)
        .add("ready=" + ready)
        .add("username=" + username)
        .toString();
  }

  public void toggleReady() {
    ready = !ready;
  }
}

package nu.borjessons.airhockeyserver.model;

import java.util.Objects;
import java.util.StringJoiner;

public class Player {
  private final Agency agency;
  private boolean ready;
  private final Username username;

  public Player(Agency agency, Username username) {
    Objects.requireNonNull(username, "username must not be null");
    Objects.requireNonNull(agency, "agency must not be null");

    this.agency = agency;
    this.username = username;
    this.ready = false;
  }

  public Agency getAgency() {
    return agency;
  }

  public Username getUsername() {
    return username;
  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;

    Player other = (Player) object;

    return username.equals(other.username);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Player.class.getSimpleName() + "[", "]")
        .add("agency=" + agency)
        .add("ready=" + ready)
        .add("username=" + username)
        .toString();
  }

  public boolean isPlayer(Username username) {
    return this.username.equals(username);
  }

  public boolean isReady() {
    return ready;
  }

  public void toggleReady() {
    ready = !ready;
  }
}
